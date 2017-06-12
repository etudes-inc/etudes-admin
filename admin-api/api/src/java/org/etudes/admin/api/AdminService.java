/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/admin/trunk/admin-api/api/src/java/org/etudes/admin/api/AdminService.java $
 * $Id: AdminService.java 4350 2013-02-06 00:01:57Z ggolden $
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

package org.etudes.admin.api;

import org.sakaiproject.exception.PermissionException;

/**
 * AdminService provides support for administrative tasks.
 */
public interface AdminService
{
	/**
	 * Add the "Blocked" role, a copy of the "Student" role minus the "site.visit", to the sites in the term
	 * 
	 * @param termId
	 *        The term id.
	 * @throws PermissionException
	 *         If the user does not have permission (must be admin).
	 */
	void addBlockedRoleTerm(final String termId) throws PermissionException;

	/**
	 * Make sure the sites in the term have the standard tools.
	 * 
	 * @param termId
	 *        The term id.
	 * @throws PermissionException
	 *         If the user does not have permission (must be admin).
	 */
	void addStdToolsToSitesInTerm(final String termId) throws PermissionException;

	/**
	 * Get the status of the running task.
	 * 
	 * @return The running task status message, or null if there's no running task.
	 */
	String getTaskStatus();

	/**
	 * Index (or re-index) all the sites in the term.
	 * 
	 * @param termId
	 *        The term id.
	 * @throws PermissionException
	 *         If the user does not have permission (must be admin).
	 */
	void indexSitesInTerm(final String termId) throws PermissionException;

	/**
	 * Check if a task is currently running.
	 * 
	 * @return true if a task is running, false if not.
	 */
	boolean isTaskRunning();
}
