/*******************************************************************************
 * Copyright (c) 2014 Zend Technologies Ltd. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * 	Contributors:
 * 		 Red Hat Inc. - initial API and implementation and/or initial documentation
 * 		 Zend Technologies Ltd. - initial implementation
 *******************************************************************************/
package org.eclipse.thym.wp.internal.core;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.osgi.util.NLS;
import org.eclipse.thym.core.HybridMobileStatus;
import org.eclipse.thym.core.HybridProject;
import org.eclipse.thym.core.HybridProjectLaunchConfigConstants;
import org.eclipse.thym.wp.core.WPCore;
import org.eclipse.thym.wp.core.vstudio.MSBuild;
import org.eclipse.thym.wp.core.vstudio.WPConstants;
import org.eclipse.thym.wp.core.vstudio.WPEmulator;

/**
 * Launch delegate for Windows Phone 8 applications.
 * 
 * @author Wojciech Galanciak, 2014
 *
 */

public class WPLaunchDelegate implements ILaunchConfigurationDelegate2 {

	private File buildArtifact;

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(Messages.WPLaunchDelegate_LaunchTask, 10);
		IProject kernelProject = getProject(configuration);
		Assert.isNotNull(kernelProject,
				Messages.WPLaunchDelegate_NoProjectError);
		int deviceId = configuration.getAttribute(
				WPConstants.ATTR_DEVICE_IDENTIFIER, -1);
		WPEmulator emulator = new WPEmulator(WPCore.getSDKLocation());
		HybridProject project = HybridProject.getHybridProject(kernelProject);
		if (project == null) {
			throw new CoreException(new Status(IStatus.ERROR, WPCore.PLUGIN_ID,
					NLS.bind(Messages.WPLaunchDelegate_NotHybridError,
							kernelProject.getName())));
		}
		String[] envp = DebugPlugin.getDefault().getLaunchManager()
				.getEnvironment(configuration);
		emulator.setProcessEnvironmentVariables(envp).emulate(buildArtifact,
				deviceId);
		monitor.worked(2);
		monitor.done();
	}

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode)
			throws CoreException {
		return null;
	}

	@SuppressWarnings("restriction")
	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		MSBuild build = new MSBuild();
		build.setLaunchConfiguration(configuration);
		build.init(getProject(configuration), null);
		build.buildNow(monitor);
		buildArtifact = build.getBuildArtifact();
		return false;
	}

	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		monitor.done();
		return true;
	}

	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		// check if SDK is still available
		String sdkLocation = WPCore.getSDKLocation();
		if (sdkLocation == null) {
			throw new CoreException(new Status(IStatus.ERROR, WPCore.PLUGIN_ID,
					Messages.WPLaunchDelegate_SDKMissingMessage));
		}
		WPEmulator emluator = new WPEmulator(sdkLocation);
		Map<String, Integer> devices = emluator.getDevices();
		if (devices == null || devices.isEmpty()) {
			throw new CoreException(new HybridMobileStatus(IStatus.ERROR,
					WPCore.PLUGIN_ID,
					WPConstants.MISSING_EMULATORS_STATUS_CODE,
					MessageFormat.format(
							Messages.WPLaunchDelegate_NoEmulatorsError,
							WPConstants.SDK_DOWNLOAD_URL), null));
		}
		return true;
	}

	private IProject getProject(ILaunchConfiguration configuration) {
		try {
			String projectName = configuration.getAttribute(
					HybridProjectLaunchConfigConstants.ATTR_BUILD_SCOPE,
					(String) null);
			if (projectName != null) {
				return ResourcesPlugin.getWorkspace().getRoot()
						.getProject(projectName);
			}
		} catch (CoreException e) {
			return null;
		}
		return null;
	}

}
