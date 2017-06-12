/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/admin/trunk/admin-tool/tool/src/java/org/etudes/admin/tool/ProgressView.java $
 * $Id: ProgressView.java 426 2010-02-04 02:19:24Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2010 Etudes, Inc.
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
import org.etudes.ambrosia.util.ControllerImpl;
import org.sakaiproject.authz.api.SecurityService;

/**
 * The /progress view for the admin tool.
 */
public class ProgressView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(ProgressView.class);

	/** The AdminService. */
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

		// check the task and message
		context.put("running", Boolean.valueOf(this.adminService.isTaskRunning()));
		context.put("message", this.adminService.getTaskStatus());

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

		// not expecting a post
		throw new IllegalArgumentException();
	}

	/**
	 * Set the AdminService.
	 * 
	 * @param service
	 *        the AdminService.
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
