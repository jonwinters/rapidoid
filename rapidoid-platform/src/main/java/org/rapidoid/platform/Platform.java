package org.rapidoid.platform;

/*
 * #%L
 * rapidoid-platform
 * %%
 * Copyright (C) 2014 - 2017 Nikolche Mihajlovski and contributors
 * %%
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
 * #L%
 */

import org.rapidoid.AuthBootstrap;
import org.rapidoid.RapidoidThing;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.commons.Arr;
import org.rapidoid.commons.RapidoidInfo;
import org.rapidoid.config.ConfigHelp;
import org.rapidoid.deploy.AppDeployer;
import org.rapidoid.deploy.AppDownloader;
import org.rapidoid.log.Log;
import org.rapidoid.performance.BenchmarkCenter;
import org.rapidoid.setup.App;
import org.rapidoid.setup.On;
import org.rapidoid.setup.PreApp;
import org.rapidoid.setup.Setup;
import org.rapidoid.u.U;
import org.rapidoid.util.AppInfo;
import org.rapidoid.util.Msc;
import org.rapidoid.util.MscOpts;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.List;

@Authors("Nikolche Mihajlovski")
@Since("5.3.0")
public class Platform extends RapidoidThing {

	static void start(String[] args, @SuppressWarnings("unused") boolean defaults) {

		initializePlatform();

		if (U.notEmpty(args)) interceptSpecialCommands(args);

		Msc.printRapidoidBanner();

		printArgs(args, defaults);

		startPlatformAndProcessArgs(args);

		AppDeployer.bootstrap();

		if (!Setup.isAnyRunning()) {
			On.setup().activate();
		}

		AuthBootstrap.bootstrapAdminCredentials();

		openInBrowser();
	}

	private static void printArgs(String[] args, boolean defaults) {
		// don't print the args when displaying help
		if (ConfigHelp.isHelpRequested(args)) return;

		if (defaults) {
			Log.info("No command-line arguments were specified, using the defaults:");
		} else {
			Log.info("Command-line arguments:");
		}

		for (String arg : args) {
			Log.info("  " + arg);
		}
		Log.info("");
	}

	private static void interceptSpecialCommands(String[] args) {

		String cmd = args[0];
		String[] cmdArgs = Arr.sub(args, 1, args.length);

		switch (cmd) {
			case "mvn":
				// interpret the "mvn" command
				int result = MavenUtil.build("/app", "/data/.m2/repository", U.list(cmdArgs));
				System.exit(result);
				break;

			case "benchmark":
				// interpret the "benchmark" command
				BenchmarkCenter.main(cmdArgs);
				System.exit(0);
				break;

			case "password":
				// interpret the "password" command
				PasswordHashTool.generatePasswordHash(cmdArgs);
				System.exit(0);
				break;
		}
	}

	private static void initializePlatform() {
		Msc.setPlatform(true);

		Log.options().prefix("[PLATFORM] ");
		Log.options().inferCaller(false);
		Log.options().showThread(false);
	}

	private static void startPlatformAndProcessArgs(String[] args) {
		List<String> normalArgs = U.list();
		List<String> appRefs = U.list();

		separateArgs(args, normalArgs, appRefs);

		PreApp.args(U.arrayOf(String.class, normalArgs));
		App.boot();

		for (String appRef : appRefs) {
			new File(MscOpts.appsPath()).mkdirs();
			AppDownloader.download(appRef, MscOpts.appsPath());
			MavenUtil.findAndBuildAndDeploy(MscOpts.appsPath());
		}
	}

	private static void separateArgs(String[] args, List<String> normalArgs, List<String> appRefs) {
		for (String arg : args) {
			if (arg.startsWith("@")) {
				String appRef = arg.substring(1);
				appRefs.add(appRef);
			} else {
				normalArgs.add(arg);
			}
		}
	}

	private static void openInBrowser() {
		try {
			if (Desktop.isDesktopSupported() && !RapidoidInfo.isSnapshot()) {
				Desktop.getDesktop().browse(new URI(U.frmt("http://localhost:%s/", AppInfo.appPort)));
			}
		} catch (Exception e) {
			// do nothing
		}
	}

}
