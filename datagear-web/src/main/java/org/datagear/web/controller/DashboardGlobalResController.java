/*
 * Copyright 2018-present datagear.tech
 *
 * This file is part of DataGear.
 *
 * DataGear is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * DataGear is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with DataGear.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.datagear.web.controller;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.datagear.util.FileUtil;
import org.datagear.util.IOUtil;
import org.datagear.util.KeywordMatcher;
import org.datagear.util.KeywordMatcher.MatchValue;
import org.datagear.util.StringUtil;
import org.datagear.util.dirquery.DirectoryPagingQuery;
import org.datagear.util.dirquery.DirectoryQuerySupport;
import org.datagear.util.dirquery.ResultFileInfo;
import org.datagear.util.query.PagingData;
import org.datagear.web.config.CoreConfigSupport;
import org.datagear.web.util.OperationMessage;
import org.datagear.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * 看板全局资源控制器。
 * 
 * @author datagear@163.com
 *
 */
@Controller
@RequestMapping("/dashboardGlobalRes")
public class DashboardGlobalResController extends AbstractController implements ServletContextAware
{
	@Autowired
	@Qualifier(CoreConfigSupport.NAME_DASHBOARD_GLOBAL_RES_ROOT_DIRECTORY)
	private File dashboardGlobalResRootDirectory;

	@Autowired
	private File tempDirectory;

	private KeywordMatcher keywordMatcher = new KeywordMatcher();

	private ServletContext servletContext;

	public DashboardGlobalResController()
	{
		super();
	}

	public File getDashboardGlobalResRootDirectory()
	{
		return dashboardGlobalResRootDirectory;
	}

	public void setDashboardGlobalResRootDirectory(File dashboardGlobalResRootDirectory)
	{
		this.dashboardGlobalResRootDirectory = dashboardGlobalResRootDirectory;
	}

	public File getTempDirectory()
	{
		return tempDirectory;
	}

	public void setTempDirectory(File tempDirectory)
	{
		this.tempDirectory = tempDirectory;
	}

	public KeywordMatcher getKeywordMatcher()
	{
		return keywordMatcher;
	}

	public void setKeywordMatcher(KeywordMatcher keywordMatcher)
	{
		this.keywordMatcher = keywordMatcher;
	}

	public ServletContext getServletContext()
	{
		return servletContext;
	}

	@Override
	public void setServletContext(ServletContext servletContext)
	{
		this.servletContext = servletContext;
	}

	@RequestMapping("/add")
	public String add(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam(value = "dir", required = false) String dir)
			throws Exception
	{
		dir = FileUtil.toDisplayPath(dir, true);

		DashboardGlobalResSaveForm form = new DashboardGlobalResSaveForm();
		form.setSavePath(dir);

		setFormModel(model, form, REQUEST_ACTION_ADD, SUBMIT_ACTION_SAVE);
		model.addAttribute("defaultDir", dir);

		return "/dashboardGlobalRes/dashboardGlobalRes_form";
	}

	@RequestMapping("/upload")
	public String upload(HttpServletRequest request, org.springframework.ui.Model model,
			@RequestParam(value = "dir", required = false) String dir)
	{
		dir = FileUtil.toDisplayPath(dir, true);

		addAttributeForWriteJson(model, "availableCharsetNames", getAvailableCharsetNames());
		model.addAttribute("zipFileNameEncodingDefault", IOUtil.CHARSET_UTF_8);
		model.addAttribute("defaultDir", dir);

		setFormAction(model, REQUEST_ACTION_UPLOAD, SUBMIT_ACTION_SAVE_UPLOAD);

		return "/dashboardGlobalRes/dashboardGlobalRes_upload";
	}

	@RequestMapping(value = "/saveUpload", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveUpload(HttpServletRequest request, HttpServletResponse response,
			@RequestBody DashboardGlobalResUploadForm form) throws Exception
	{
		if (isEmpty(form.getFilePath()))
			throw new IllegalInputException();

		File file = FileUtil.getFile(this.tempDirectory, form.getFilePath());
		String savePath = form.getSavePath();

		if (form.isAutoUnzip() && FileUtil.isExtension(file, "zip"))
		{
			File parent = this.dashboardGlobalResRootDirectory;
			if (!StringUtil.isEmpty(savePath))
				parent = FileUtil.getDirectory(this.dashboardGlobalResRootDirectory, savePath, true);

			ZipInputStream in = null;

			try
			{
				in = IOUtil.getZipInputStream(file, form.getZipFileNameEncoding());
				IOUtil.unzipCheckMalformed(in, parent);
			}
			finally
			{
				IOUtil.close(in);
			}
		}
		else
		{
			if (isEmpty(form.getSavePath()))
				throw new IllegalInputException();

			File resFile = FileUtil.getFile(this.dashboardGlobalResRootDirectory, savePath, true);
			IOUtil.copy(file, resFile);
		}

		return optSuccessResponseEntity(request);
	}

	@RequestMapping(value = "/uploadFile", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public Map<String, Object> uploadFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("file") MultipartFile multipartFile) throws Exception
	{
		File tmpDirectory = FileUtil.generateUniqueDirectory(this.tempDirectory);
		String fileName = multipartFile.getOriginalFilename();
		File file = FileUtil.getFile(tmpDirectory, fileName);

		writeMultipartFile(multipartFile, file);

		String uploadFilePath = FileUtil.getRelativePath(this.tempDirectory, file);

		Map<String, Object> results = new HashMap<>();
		results.put("filePath", uploadFilePath);
		results.put("fileName", fileName);

		return results;
	}

	@RequestMapping("/edit")
	public String edit(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam("path") String path) throws Exception
	{
		File file = FileUtil.getFile(this.dashboardGlobalResRootDirectory, path);

		if (!file.exists())
			throw new RecordNotFoundException();

		String resourceContent = IOUtil.readString(IOUtil.getBufferedInputStream(IOUtil.getInputStream(file)),
				IOUtil.CHARSET_UTF_8, true);

		DashboardGlobalResSaveForm formModel = new DashboardGlobalResSaveForm();
		formModel.setSavePath(path);
		formModel.setInitSavePath(path);
		formModel.setResourceContent(resourceContent);
		
		setFormModel(model, formModel, REQUEST_ACTION_EDIT, SUBMIT_ACTION_SAVE);

		return "/dashboardGlobalRes/dashboardGlobalRes_form";
	}

	@RequestMapping(value = "/save", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveEdit(HttpServletRequest request, HttpServletResponse response,
			@RequestBody DashboardGlobalResSaveForm form)
			throws Exception
	{
		if(isEmpty(form.getSavePath()))
			throw new IllegalInputException();
		
		File file = FileUtil.getFile(this.dashboardGlobalResRootDirectory, form.getSavePath(), true);

		Reader in = null;
		Writer out = null;

		try
		{
			in = IOUtil.getReader(form.getResourceContent());
			out = IOUtil.getWriter(file, IOUtil.CHARSET_UTF_8);

			IOUtil.write(in, out);
		}
		finally
		{
			IOUtil.close(in);
			IOUtil.close(out);
		}

		if (!StringUtil.isEmpty(form.getInitSavePath()) && !form.getInitSavePath().equalsIgnoreCase(form.getSavePath()))
		{
			File initFile = FileUtil.getFile(this.dashboardGlobalResRootDirectory, form.getInitSavePath());
			FileUtil.deleteFile(initFile);
		}

		return optSuccessResponseEntity(request);
	}

	@RequestMapping("/rename")
	public String rename(HttpServletRequest request, Model model, @RequestParam("path") String path)
	{
		File file = FileUtil.getFile(this.dashboardGlobalResRootDirectory, path, false);
		FileRenameForm form = new FileRenameForm(path, file.getName());

		setFormModel(model, form, REQUEST_ACTION_EDIT, "saveRename");

		return "/dashboardGlobalRes/dashboardGlobalRes_rename";
	}

	@RequestMapping(value = "/saveRename", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveRename(HttpServletRequest request, Model model,
			@RequestBody FileRenameForm form) throws Exception
	{
		if (isEmpty(form.getPath()) || isEmpty(form.getName()) || FileUtil.hasPathSeparator(form.getName()))
			throw new IllegalInputException();

		File file = FileUtil.getFile(this.dashboardGlobalResRootDirectory, form.getPath(), false);

		// 文件不存在忽略即可
		if (file.exists())
		{
			if (FileUtil.getSibling(file, form.getName()).exists())
			{
				return optFailResponseEntity(request, "file.error.targetFileExists");
			}
			else
			{
				FileUtil.rename(file, form.getName());
			}
		}

		return optSuccessResponseEntity(request);
	}

	@RequestMapping("/move")
	public String move(HttpServletRequest request, Model model, @RequestParam("path") String path)
	{
		File file = FileUtil.getFile(this.dashboardGlobalResRootDirectory, path, false);

		String targetDir = path;
		if (file.exists() && !file.isDirectory())
		{
			File parent = file.getParentFile();
			targetDir = FileUtil.getRelativePath(this.dashboardGlobalResRootDirectory, parent);
		}

		targetDir = FileUtil.toDisplayPath(targetDir, true, true);

		FileMoveForm form = new FileMoveForm(path, targetDir);

		setFormModel(model, form, REQUEST_ACTION_EDIT, "saveMove");

		return "/dashboardGlobalRes/dashboardGlobalRes_move";
	}

	@RequestMapping(value = "/saveMove", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveMove(HttpServletRequest request, Model model,
			@RequestBody FileMoveForm form) throws Exception
	{
		if (isEmpty(form.getPath()) || isEmpty(form.getDirectory()))
			throw new IllegalInputException();

		if (FileUtil.toDisplayPath(form.getDirectory(), true, true)
				.startsWith(FileUtil.toDisplayPath(form.getPath(), false)))
		{
			return optFailResponseEntity(request, "file.error.moveToSubDirNotAllowed");
		}

		File file = FileUtil.getFile(this.dashboardGlobalResRootDirectory, form.getPath(), false);

		// 文件不存在忽略即可
		if (file.exists())
		{
			File targetDirectory = FileUtil.getFile(this.dashboardGlobalResRootDirectory, form.getDirectory(), false);

			if (!targetDirectory.exists())
				targetDirectory = FileUtil.getDirectory(this.dashboardGlobalResRootDirectory, form.getDirectory(),
						true);

			if (!targetDirectory.isDirectory())
			{
				return optFailResponseEntity(request, "file.error.tagetFileNotDir");
			}

			if (FileUtil.getFile(targetDirectory, file.getName()).exists())
			{
				return optFailResponseEntity(request, "file.error.fileExistsInTargetDir");
			}

			FileUtil.moveToDir(file, targetDirectory);
		}

		return optSuccessResponseEntity(request);
	}

	@RequestMapping("/view")
	public void view(HttpServletRequest request, HttpServletResponse response, WebRequest webRequest,
			org.springframework.ui.Model model, @RequestParam("path") String path) throws Exception
	{
		path = WebUtils.decodeURL(path);

		File file = FileUtil.getFile(this.dashboardGlobalResRootDirectory, path);

		if (!file.exists())
			throw new FileNotFoundException(path);

		InputStream in = null;

		if (file.exists() && !file.isDirectory())
		{
			setContentTypeByName(request, response, getServletContext(), file.getName());
			in = IOUtil.getInputStream(file);
		}

		if (in != null)
		{
			OutputStream out = null;

			try
			{
				in = IOUtil.getBufferedInputStream(in);
				out =IOUtil.getBufferedOutputStream(response.getOutputStream());
				IOUtil.write(in, out);
			}
			finally
			{
				IOUtil.close(in);
				IOUtil.close(out);
			}
		}
		else
			throw new FileNotFoundException(path);
	}

	@RequestMapping(value = "/download")
	public void downloadFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("path") String path) throws Exception
	{
		File file = FileUtil.getFile(this.dashboardGlobalResRootDirectory, path);

		if (!file.exists())
			throw new RecordNotFoundException();

		String responseFileName = file.getName();

		if (file.isDirectory())
		{
			if (!FileUtil.isExtension(responseFileName, "zip"))
				responseFileName += ".zip";
		}

		response.setCharacterEncoding(IOUtil.CHARSET_UTF_8);
		setDownloadResponseHeader(request, response, responseFileName);
		OutputStream out = response.getOutputStream();

		if (file.isDirectory())
		{
			ZipOutputStream zout = null;

			try
			{
				zout = IOUtil.getZipOutputStream(out);
				IOUtil.writeFileToZipOutputStream(zout, file, file.getName());
			}
			finally
			{
				IOUtil.flush(zout);
				IOUtil.close(zout);
			}
		}
		else
		{
			IOUtil.write(file, out);
		}
	}

	@RequestMapping(value = "/delete", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> delete(HttpServletRequest request, HttpServletResponse response,
			@RequestBody String[] paths)
	{
		for (int i = 0; i < paths.length; i++)
		{
			File file = FileUtil.getFile(this.dashboardGlobalResRootDirectory, paths[i]);
			FileUtil.deleteFile(file);
		}

		return optSuccessResponseEntity(request);
	}

	@RequestMapping("/manage")
	public String manage(HttpServletRequest request, org.springframework.ui.Model model)
	{
		model.addAttribute(KEY_REQUEST_ACTION, REQUEST_ACTION_MANAGE);
		setReadonlyAction(model);
		return "/dashboardGlobalRes/dashboardGlobalRes_table";
	}

	@RequestMapping("/select")
	public String select(HttpServletRequest request, org.springframework.ui.Model model,
			@RequestParam(value = "onlyDirectory", required = false) String onlyDirectory)
	{
		setSelectAction(request, model);
		model.addAttribute("onlyDirectory", StringUtil.toBoolean(onlyDirectory));

		return "/dashboardGlobalRes/dashboardGlobalRes_table";
	}

	@RequestMapping(value = "/pagingQueryData", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public PagingData<ResultFileInfo> pagingQueryData(HttpServletRequest request, HttpServletResponse response,
			final org.springframework.ui.Model springModel,
			@RequestBody(required = false) DirectoryPagingQuery pagingQueryParam)
			throws Exception
	{
		final DirectoryPagingQuery pagingQuery = inflateDirectoryPagingQuery(request, pagingQueryParam);
		DirectoryQuerySupport qs = getDirectoryQuerySupport();

		return qs.pagingQuery(pagingQuery);
	}

	protected DirectoryQuerySupport getDirectoryQuerySupport()
	{
		return new DirectoryQuerySupport(getDashboardGlobalResRootDirectory());
	}

	protected List<DashboardGlobalResItem> findDashboardGlobalResItems(String keyword)
	{
		List<File> files = new ArrayList<>();
		listAllDescendentFiles(this.dashboardGlobalResRootDirectory, files);

		List<DashboardGlobalResItem> resItems = new ArrayList<>(files.size());

		for (File file : files)
		{
			DashboardGlobalResItem item = toDashboardGlobalResItem(file);
			resItems.add(item);
		}

		if (StringUtil.isEmpty(keyword))
			return resItems;

		return this.keywordMatcher.match(resItems, keyword, new MatchValue<DashboardGlobalResItem>()
				{
					@Override
					public String[] get(DashboardGlobalResItem t)
					{
						return new String[] { t.getPath() };
					}
				});
	}

	protected DashboardGlobalResItem toDashboardGlobalResItem(File file)
	{
		String path = FileUtil.trimPath(FileUtil.getRelativePath(this.dashboardGlobalResRootDirectory, file),
				FileUtil.PATH_SEPARATOR_SLASH);

		if (file.isDirectory() && !path.endsWith(FileUtil.PATH_SEPARATOR_SLASH))
			path += FileUtil.PATH_SEPARATOR_SLASH;

		DashboardGlobalResItem item = new DashboardGlobalResItem(path);

		return item;
	}

	/**
	 * 列出所有嵌套目录、文件夹。
	 * 
	 * @param directory
	 * @param files
	 */
	protected void listAllDescendentFiles(File directory, List<File> files)
	{
		if (!directory.exists())
			return;

		File[] children = directory.listFiles();

		Arrays.sort(children, new Comparator<File>()
		{
			@Override
			public int compare(File o1, File o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});

		for (File child : children)
		{
			files.add(child);

			if (child.isDirectory())
				listAllDescendentFiles(child, files);
		}
	}

	public static class DashboardGlobalResUploadForm implements ControllerForm
	{
		private static final long serialVersionUID = 1L;

		private String filePath;

		private String fileName;

		/** 是否自动解压zip文件 */
		private boolean autoUnzip = false;

		/** 存储路径 */
		private String savePath = "";

		private String zipFileNameEncoding;

		public DashboardGlobalResUploadForm()
		{
			super();
		}

		public DashboardGlobalResUploadForm(String filePath, String fileName)
		{
			super();
			this.filePath = filePath;
			this.fileName = fileName;
		}

		public String getFilePath()
		{
			return filePath;
		}

		public void setFilePath(String filePath)
		{
			this.filePath = filePath;
		}

		public String getFileName()
		{
			return fileName;
		}

		public void setFileName(String fileName)
		{
			this.fileName = fileName;
		}

		public boolean isAutoUnzip()
		{
			return autoUnzip;
		}

		public void setAutoUnzip(boolean autoUnzip)
		{
			this.autoUnzip = autoUnzip;
		}

		public String getSavePath()
		{
			return savePath;
		}

		public void setSavePath(String savePath)
		{
			this.savePath = savePath;
		}

		public String getZipFileNameEncoding()
		{
			return zipFileNameEncoding;
		}

		public void setZipFileNameEncoding(String zipFileNameEncoding)
		{
			this.zipFileNameEncoding = zipFileNameEncoding;
		}
	}

	public static class DashboardGlobalResItem implements ControllerForm
	{
		private static final long serialVersionUID = 1L;

		/** 相对路径 */
		private String path;

		public DashboardGlobalResItem()
		{
			super();
		}

		public DashboardGlobalResItem(String path)
		{
			super();
			this.path = path;
		}

		public String getPath()
		{
			return path;
		}

		public void setPath(String path)
		{
			this.path = path;
		}
	}

	public static class DashboardGlobalResSaveForm implements ControllerForm
	{
		private static final long serialVersionUID = 1L;

		private String savePath;
		private String resourceContent = "";
		private String initSavePath = null;

		public DashboardGlobalResSaveForm()
		{
			super();
		}

		public String getSavePath()
		{
			return savePath;
		}

		public void setSavePath(String savePath)
		{
			this.savePath = savePath;
		}

		public String getResourceContent()
		{
			return resourceContent;
		}

		public void setResourceContent(String resourceContent)
		{
			this.resourceContent = resourceContent;
		}

		public String getInitSavePath()
		{
			return initSavePath;
		}

		public void setInitSavePath(String initSavePath)
		{
			this.initSavePath = initSavePath;
		}
	}

	public static class FileRenameForm implements ControllerForm
	{
		private static final long serialVersionUID = 1L;

		/** 原路径 */
		private String path;

		/** 新名称 */
		private String name;

		public FileRenameForm()
		{
			super();
		}

		public FileRenameForm(String path, String name)
		{
			super();
			this.path = path;
			this.name = name;
		}

		public String getPath()
		{
			return path;
		}

		public void setPath(String path)
		{
			this.path = path;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}
	}

	public static class FileMoveForm implements ControllerForm
	{
		private static final long serialVersionUID = 1L;

		/** 原路径 */
		private String path;

		/** 目标目录 */
		private String directory;

		public FileMoveForm()
		{
			super();
		}

		public FileMoveForm(String path, String directory)
		{
			super();
			this.path = path;
			this.directory = directory;
		}

		public String getPath()
		{
			return path;
		}

		public void setPath(String path)
		{
			this.path = path;
		}

		public String getDirectory()
		{
			return directory;
		}

		public void setDirectory(String directory)
		{
			this.directory = directory;
		}
	}
}
