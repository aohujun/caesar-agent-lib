package com.hupu.msv.apm.plugin.boot.loader.v1;


import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author tanghengjie
 */
public class ExecutableArchiveLauncherInterceptor implements InstanceMethodsAroundInterceptor {

	private static final ILog logger = LogManager.getLogger(ExecutableArchiveLauncherInterceptor.class);

	private static final String NESTED_ARCHIVE_SEPARATOR = "!" + File.separator;

	@Override
	public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

	}

	@Override
	public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
		/**
		 *
		 * ExecutableArchiveLauncher 的 getClassPathArchives 方法执行完之后， 会返回一个 List<Archive>， 然后就会基于这个集合去装载classloader，
		 * 故，如果想要添加用户没有依赖的 jar 包， 想让其在同一个 classpath 中生效， 只要在运行时将自定义目标下的所有的 jar 包导入 List<Archive> 即可
		 *
		 * */
		List<Archive> lib = (List<Archive>)ret;

		String userDir = System.getProperty("user.dir");
		logger.debug("###user.dir: {} ", userDir);
		logger.debug("###current.path: {}", Paths.get("").toAbsolutePath().toString());

		String pluginExtClassPath = Config.Agent.DEPENDENCY_EXT_CLASS_PATH;

		logger.debug("###ext class path: {}", pluginExtClassPath);

		if(pluginExtClassPath == null || "".equals(pluginExtClassPath)){
			logger.error("Plugin dependent extend classpath is not set. Prometheus's endpoint and all monitor functions will not be available. Please set agent.dependency_ext_class_path");
			return ret;
		}

		List<Archive> archives = getClassPathArchives(pluginExtClassPath);
		for (Archive archive : archives) {
			if (archive instanceof ExplodedArchive) {
				List<Archive> nested = new ArrayList<>(archive.getNestedArchives(new ArchiveEntryFilter()));
				nested.add(0, archive);
				lib.addAll(nested);
			}
			else {
				lib.add(archive);
			}
		}

		ret = lib;

		return ret;
	}

	@Override
	public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

	}

	private List<Archive> getClassPathArchives(String path) throws Exception {
		String root = cleanupPath(handleUrl(path));
		logger.debug("###root: {}", root);
		List<Archive> lib = new ArrayList<>();
		File file = new File(root);
		if (!"/".equals(root)) {
			if (file.isDirectory()) {
				logger.debug("Adding classpath entries from " + file);
				Archive archive = new ExplodedArchive(file, false);
				lib.add(archive);
			}
		}

		Archive archive = getArchive(file);
		if (archive != null) {
			logger.debug("Adding classpath entries from archive " + archive.getUrl() + root);
			lib.add(archive);
		}

		return lib;
	}

	private String cleanupPath(String path) {
		path = path.trim();
		// No need for current dir path
		if (path.startsWith("./")) {
			path = path.substring(2);
		}
		String lowerCasePath = path.toLowerCase(Locale.ENGLISH);
		if (lowerCasePath.endsWith(".jar") || lowerCasePath.endsWith(".zip")) {
			return path;
		}
		if (path.endsWith("/*")) {
			path = path.substring(0, path.length() - 1);
		}
		else {
			// It's a directory
			if (!path.endsWith("/") && !path.equals(".")) {
				path = path + "/";
			}
		}
		return path;
	}

	private String handleUrl(String path) throws UnsupportedEncodingException {
		if (path.startsWith("jar:file:") || path.startsWith("file:")) {
			path = URLDecoder.decode(path, "UTF-8");
			if (path.startsWith("file:")) {
				path = path.substring("file:".length());
				if (path.startsWith("//")) {
					path = path.substring(2);
				}
			}
		}
		return path;
	}

	private boolean isAbsolutePath(String root) {
		// Windows contains ":" others start with "/"
		return root.contains(":") || root.startsWith("/");
	}

	private Archive getArchive(File file) throws IOException {
		if (isNestedArchivePath(file)) {
			return null;
		}
		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.endsWith(".jar") || name.endsWith(".zip")) {
			return new JarFileArchive(file);
		}
		return null;
	}

	private boolean isNestedArchivePath(File file) {
		return file.getPath().contains(NESTED_ARCHIVE_SEPARATOR);
	}


	/**
	 * Convenience class for finding nested archives (archive entries that can be
	 * classpath entries).
	 */
	private static final class ArchiveEntryFilter implements Archive.EntryFilter {

		private static final String DOT_JAR = ".jar";

		private static final String DOT_ZIP = ".zip";

		@Override
		public boolean matches(Archive.Entry entry) {
			logger.info(">>> {}", entry.getName());
			return entry.getName().endsWith(DOT_JAR) || entry.getName().endsWith(DOT_ZIP);
		}
	}
}
