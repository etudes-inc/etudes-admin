/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/admin/trunk/admin-tool/tool/src/java/org/etudes/admin/tool/HomeView.java $
 * $Id: HomeView.java 4350 2013-02-06 00:01:57Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2010, 2011, 2012, 2013 Etudes, Inc.
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

package org.etudes.admin.tool;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.admin.api.AdminService;
import org.etudes.ambrosia.api.Context;
import org.etudes.ambrosia.api.Value;
import org.etudes.ambrosia.util.ControllerImpl;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.util.Web;

/**
 * The /home view for the admin tool.
 */
public class HomeView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(HomeView.class);

	/** Dependency: AdminService. */
	protected AdminService adminService = null;

	/** Dependency: SecurityService. */
	protected SecurityService securityService = null;

	/**
	 * Shutdown.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void get(HttpServletRequest req, HttpServletResponse res, Context context, String[] params)
	{
		// if not logged in as the super user, we won't do anything
		if (!securityService.isSuperUser())
		{
			throw new IllegalArgumentException();
		}

		// no parameters expected
		if (params.length != 2)
		{
			throw new IllegalArgumentException();
		}

		// render
		uiService.render(ui, context);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		if (!context.getPostExpected())
		{
			throw new IllegalArgumentException();
		}

		// no parameters expected
		if (params.length != 2)
		{
			throw new IllegalArgumentException();
		}

		// data for the form
		Value blockedTermValue = this.uiService.newValue();
		context.put("blockedTermValue", blockedTermValue);
		Value toolsTermValue = this.uiService.newValue();
		context.put("toolsTermValue", toolsTermValue);
		Value indexTermValue = this.uiService.newValue();
		context.put("indexTermValue", indexTermValue);

		// read form
		String destination = uiService.decode(req, context);

		if ("BLOCKED_TERM".equals(destination))
		{
			try
			{
				this.adminService.addBlockedRoleTerm(blockedTermValue.getValue());

				// go to the progress view
				destination = "/progress";
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
			catch (PermissionException e)
			{
				throw new IllegalArgumentException();
			}
		}

		else if ("TOOLS_TERM".equals(destination))
		{
			try
			{
				this.adminService.addStdToolsToSitesInTerm(toolsTermValue.getValue());

				// go to the progress view
				destination = "/progress";
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
			catch (PermissionException e)
			{
				throw new IllegalArgumentException();
			}
		}

		else if ("INDEX_TERM".equals(destination))
		{
			try
			{
				this.adminService.indexSitesInTerm(indexTermValue.getValue());

				// go to the progress view
				destination = "/progress";
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
			catch (PermissionException e)
			{
				throw new IllegalArgumentException();
			}
		}

		// otherwise
		destination = "/home";
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * Set the AdminService.
	 * 
	 * @param service
	 *        The AdminService.
	 */
	public void setAdminService(AdminService service)
	{
		this.adminService = service;
	}

	/**
	 * Set the security service.
	 * 
	 * @param service
	 *        The security service.
	 */
	public void setSecurityService(SecurityService service)
	{
		this.securityService = service;
	}
}
