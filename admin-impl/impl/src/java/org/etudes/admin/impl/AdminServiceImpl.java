/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/admin/trunk/admin-impl/impl/src/java/org/etudes/admin/impl/AdminServiceImpl.java $
 * $Id: AdminServiceImpl.java 9145 2014-10-31 21:54:16Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2010, 2011, 2012. 2014 Etudes, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.etudes.admin.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.admin.api.AdminService;
//import org.etudes.search.api.SearchService;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.AuthzPermissionException;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.RoleAlreadyDefinedException;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;

/**
 * AdminServiceImpl implements AdminService
 */
public class AdminServiceImpl implements AdminService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AdminServiceImpl.class);

	/** The AuthzGroupService. */
	protected AuthzGroupService azgService = null;

	/** Dependency: SecurityService */
	protected SecurityService securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

	/** The SiteService. */
	protected SiteService siteService = null;

	/** MS to sleep between multi-site tasks, after each site. */
	protected final int SLEEPY_TIME = 100;

	/** Dependency: SqlService. */
	protected SqlService sqlService = null;

	/** The task thread. */
	protected Thread task = null;

	/** The task status. */
	protected StringBuilder taskStatusBuf = new StringBuilder(1024);

	/** Semaphore for task data. */
	protected Object taskSync = new Object();

	/** Dependency: SessionManager */
	protected ThreadLocalManager threadLocalManager = null;

	/**
	 * {@inheritDoc}
	 */
	public void addBlockedRoleTerm(final String termId) throws PermissionException
	{
		if (!this.securityService.isSuperUser()) throw new PermissionException("", "", ""); // /TODO: user, lock, resource

		if ((termId == null) || (termId.length() == 0))
		{
			this.messageNoLog("addBlockedRoleTerm: please specify a valid term id");
			return;
		}

		// get the current user
		final String userId = this.sessionManager.getCurrentSessionUserId();

		synchronized (this.taskSync)
		{
			if (this.task != null)
			{
				return;
			}

			this.task = new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						// set the user into the thread
						Session s = sessionManager.getCurrentSession();
						if (s != null)
						{
							s.setUserId(userId);
						}

						pushAdvisor();

						message("addBlockedRoleTerm: ", termId);

						// get the sites for the term
						List<String> siteIds = getTermSiteIds(termId);
						if (siteIds.isEmpty())
						{
							synchronized (taskSync)
							{
								task = null;
								message("addBlockedRoleTerm: ", termId + " has no sites");
							}

							return;
						}

						int siteNum = 0;
						int numSites = siteIds.size();

						for (String siteId : siteIds)
						{
							siteNum++;

							message("addBlockedRoleTerm: site ", siteNum, " of ", numSites, " : ", siteId);

							// form the authz group id for the site
							String azgId = "/site/" + siteId;

							installBlockedRole(azgId);

							sleep();
						}

						synchronized (taskSync)
						{
							task = null;
							message("addBlockedRoleTerm: term: ", termId, " sites processed ", numSites);
						}
					}

					finally
					{
						popAdvisor();

						if (task != null)
						{
							task = null;
							messageAppendNoLog(" *** Failed ***");
						}

						threadLocalManager.clear();
					}
				}
			}, getClass().getName());
		}

		task.start();
	}

	/**
	 * {@inheritDoc}
	 */
	public void addStdToolsToSitesInTerm(final String termId) throws PermissionException
	{
		if (!this.securityService.isSuperUser()) throw new PermissionException("", "", ""); // /TODO: user, lock, resource

		if ((termId == null) || (termId.length() == 0))
		{
			this.messageNoLog("addStdToolsToSitesInTerm: please specify a valid term id");
			return;
		}

		// get the current user
		final String userId = this.sessionManager.getCurrentSessionUserId();

		synchronized (this.taskSync)
		{
			if (this.task != null)
			{
				return;
			}

			this.task = new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						// set the user into the thread
						Session s = sessionManager.getCurrentSession();
						if (s != null)
						{
							s.setUserId(userId);
						}

						pushAdvisor();

						message("addStdToolsToSitesInTerm: ", termId);

						// get the sites for the term
						List<String> siteIds = getTermSiteIds(termId);
						if (siteIds.isEmpty())
						{
							synchronized (taskSync)
							{
								task = null;
								message("addStdToolsToSitesInTerm: ", termId + " has no sites");
							}

							return;
						}

						int siteNum = 0;
						int numSites = siteIds.size();

						for (String siteId : siteIds)
						{
							siteNum++;

							message("addStdToolsToSitesInTerm: site ", siteNum, " of ", numSites, " : ", siteId);

							assureStdTools(siteId);

							sleep();
						}

						synchronized (taskSync)
						{
							task = null;
							message("addBlockedRoleTerm: term: ", termId, " sites processed ", numSites);
						}
					}

					finally
					{
						popAdvisor();

						if (task != null)
						{
							task = null;
							messageAppendNoLog(" *** Failed ***");
						}

						threadLocalManager.clear();
					}
				}
			}, getClass().getName());
		}

		task.start();
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTaskStatus()
	{
		synchronized (this.taskSync)
		{
			return this.taskStatusBuf.toString();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void indexSitesInTerm(final String termId) throws PermissionException
	{
		if (!this.securityService.isSuperUser()) throw new PermissionException("", "", ""); // /TODO: user, lock, resource

		if ((termId == null) || (termId.length() == 0))
		{
			this.messageNoLog("indexSitesInTerm: please specify a valid term id");
			return;
		}

		// get the current user
		final String userId = this.sessionManager.getCurrentSessionUserId();
//		final SearchService searchService = this.searchService();

		synchronized (this.taskSync)
		{
			if (this.task != null)
			{
				return;
			}

			this.task = new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						// set the user into the thread
						Session s = sessionManager.getCurrentSession();
						if (s != null)
						{
							s.setUserId(userId);
						}

						pushAdvisor();

						message("indexSitesInTerm: ", termId);

						// get the sites for the term
						List<String> siteIds = getTermSiteIds(termId);
						if (siteIds.isEmpty())
						{
							synchronized (taskSync)
							{
								task = null;
								message("indexSitesInTerm: ", termId + " has no sites");
							}

							return;
						}

						int siteNum = 0;
						int numSites = siteIds.size();

						for (String siteId : siteIds)
						{
							siteNum++;

							message("indexSitesInTerm: site ", siteNum, " of ", numSites, " : ", siteId);

//							searchService.indexSite(siteId);

							sleep();
						}

						synchronized (taskSync)
						{
							task = null;
							message("indexSitesInTerm: term: ", termId, " sites processed ", numSites);
						}
					}

					finally
					{
						popAdvisor();

						if (task != null)
						{
							task = null;
							messageAppendNoLog(" *** Failed ***");
						}

						threadLocalManager.clear();
					}
				}
			}, getClass().getName());
		}

		task.start();
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			M_log.info("init()");
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTaskRunning()
	{
		synchronized (this.taskSync)
		{
			return this.task != null;
		}
	}

	/**
	 * Set the AuthzGroup.
	 * 
	 * @param service
	 *        the AuthzGroup.
	 */
	public void setAuthzGroupService(AuthzGroupService service)
	{
		this.azgService = service;
	}

	/**
	 * Dependency: SecurityService.
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		this.securityService = service;
	}

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param service
	 *        The SessionManager.
	 */
	public void setSessionManager(SessionManager service)
	{
		this.sessionManager = service;
	}

	/**
	 * Set the SiteService.
	 * 
	 * @param service
	 *        the SiteService.
	 */
	public void setSiteService(SiteService service)
	{
		this.siteService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSqlService(SqlService service)
	{
		this.sqlService = service;
	}

	/**
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The ThreadLocalManager.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		this.threadLocalManager = service;
	}

	/**
	 * Make sure the standard, required tools are in place for this site.
	 * 
	 * @param siteId
	 *        The site id.
	 */
	protected void assureStdTools(String siteId)
	{
		try
		{
			boolean changed = false;

			// get the site
			Site site = this.siteService.getSite(siteId);

			// find site info
			ToolConfiguration tool = site.getToolForCommonId("sakai.siteinfo");

			// if found, remove it
			if (tool != null)
			{
				SitePage page = site.getPage(tool.getPageId());
				site.removePage(page);
				changed = true;
			}

			// find roster
			tool = site.getToolForCommonId("e3.roster");

			// if not found, add a page with the tool
			if (tool == null)
			{
				SitePage page = site.addPage();
				page.setTitle("Roster");
				page.addTool("e3.roster");
				changed = true;
			}

			// find configure
			tool = site.getToolForCommonId("e3.configure");

			// if not found, add a page with the tool
			if (tool == null)
			{
				SitePage page = site.addPage();
				page.setTitle("Configure");
				page.addTool("e3.configure");
				changed = true;
			}

			// save if needed
			if (changed)
			{
				siteService.save(site);
			}
		}
		catch (IdUnusedException e)
		{
			M_log.warn("assureStdTools: " + e.toString());
		}
		catch (PermissionException e)
		{
			M_log.warn("assureStdTools: " + e.toString());
		}
	}

	/**
	 * Get the site ids for the term.
	 * 
	 * @param termId
	 *        The term id.
	 * @param page
	 *        The subset of the term to get.
	 * @return The List of site ids, possibly empty, for the term.
	 */
	protected List<String> getTermSiteIds(String termId)
	{
		String sql = "SELECT SITE_ID FROM ARCHIVES_SITE_TERM ST INNER JOIN ARCHIVES_TERM T ON ST.TERM_ID = T.ID WHERE T.TERM = ? ORDER BY ST.ID ASC";
		// LIMIT ?,?
		Object[] fields = new Object[1];
		fields[0] = termId;
		// fields[1] = page.getOffset();
		// fields[2] = page.getSize();

		List<String> rv = this.sqlService.dbRead(sql.toString(), fields, null);

		return rv;
	}

	/**
	 * Add Blocked role to the authzGroup.
	 * 
	 * @param azgId
	 *        The authz group id.
	 */
	protected void installBlockedRole(String azgId)
	{
		try
		{
			// get the azg
			AuthzGroup azg = azgService.getAuthzGroup(azgId);

			// if we don't already have the Blocked role
			if (azg.getRole("Blocked") == null)
			{
				// make it a copy of the Student role
				Role student = azg.getRole("Student");
				if (student != null)
				{
					Role blocked = azg.addRole("Blocked");
					blocked.setDescription("Student blocked from site access");
					blocked.allowFunctions(student.getAllowedFunctions());

					// but without the "site.visit" permission
					blocked.disallowFunction("site.visit");

					// save the site
					azgService.save(azg);

					message("azg " + azgId + " updated with Blocked role");
				}
				else
				{
					message("azg " + azgId + " does not have Student role");
				}
			}
			else
			{
				message("azg " + azgId + " already has Blocked role");
			}
		}
		catch (GroupNotDefinedException e)
		{
			message("azg " + azgId + " does not exist");
		}
		catch (AuthzPermissionException e)
		{
			M_log.warn("installBlockedRole: " + e.toString());
		}
		catch (RoleAlreadyDefinedException e)
		{
			M_log.warn("installBlockedRole: " + e.toString());
		}
	}

	/**
	 * Put out a message made up of some number of components, to the task status and log.
	 * 
	 * @param components
	 *        The message components
	 */
	protected void message(Object... components)
	{
		messageNoLog(components);

		// and log
		M_log.info(this.taskStatusBuf.toString());
	}

	/**
	 * Put out a message made up of some number of components, to the task status; add to what is there.
	 * 
	 * @param components
	 *        The message components
	 */
	protected void messageAppendNoLog(Object... components)
	{
		synchronized (this.taskSync)
		{
			// append each component
			for (Object o : components)
			{
				this.taskStatusBuf.append(o.toString());
			}
		}
	}

	/**
	 * Put out a message made up of some number of components, to the task status.
	 * 
	 * @param components
	 *        The message components
	 */
	protected void messageNoLog(Object... components)
	{
		synchronized (this.taskSync)
		{
			// start clean
			this.taskStatusBuf.setLength(0);

			// append each component
			for (Object o : components)
			{
				this.taskStatusBuf.append(o.toString());
			}
		}
	}

	/**
	 * Remove our security advisor.
	 */
	protected void popAdvisor()
	{
		this.securityService.popAdvisor();
	}

	/**
	 * Setup a security advisor.
	 */
	protected void pushAdvisor()
	{
		// setup a security advisor
		this.securityService.pushAdvisor(new SecurityAdvisor()
		{
			public SecurityAdvice isAllowed(String userId, String function, String reference)
			{
				return SecurityAdvice.ALLOWED;
			}
		});
	}

//	/**
//	 * @return The SearchService, via the component manager.
//	 */
//	protected SearchService searchService()
//	{
//		return (SearchService) ComponentManager.get(SearchService.class);
//	}

	/**
	 * Give up some time for other things.
	 */
	protected void sleep()
	{
		try
		{
			Thread.sleep(SLEEPY_TIME);
		}
		catch (InterruptedException e)
		{
		}
	}

}
