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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.datagear.management.domain.User;
import org.datagear.management.service.UserService;
import org.datagear.management.util.RoleSpec;
import org.datagear.util.IDUtil;
import org.datagear.web.config.ApplicationProperties;
import org.datagear.web.util.CheckCodeManager;
import org.datagear.web.util.DetectNewVersionScriptResolver;
import org.datagear.web.util.OperationMessage;
import org.datagear.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 用户注册控制器。
 * 
 * @author datagear@163.com
 *
 */
@Controller
@RequestMapping("/register")
public class RegisterController extends AbstractController
{
	public static final String SESSION_KEY_REGISTER_USER_NAME = "registerSuccessUserName";

	public static final String CHECK_CODE_MODULE_REGISTER = "REGISTER";

	@Autowired
	private ApplicationProperties applicationProperties;

	@Autowired
	private UserService userService;

	@Autowired
	private CheckCodeManager checkCodeManager;

	@Autowired
	private RoleSpec roleSpec;

	@Autowired
	private DetectNewVersionScriptResolver detectNewVersionScriptResolver;

	public RegisterController()
	{
		super();
	}

	public ApplicationProperties getApplicationProperties()
	{
		return applicationProperties;
	}

	public void setApplicationProperties(ApplicationProperties applicationProperties)
	{
		this.applicationProperties = applicationProperties;
	}

	public UserService getUserService()
	{
		return userService;
	}

	public void setUserService(UserService userService)
	{
		this.userService = userService;
	}

	public CheckCodeManager getCheckCodeManager()
	{
		return checkCodeManager;
	}

	public void setCheckCodeManager(CheckCodeManager checkCodeManager)
	{
		this.checkCodeManager = checkCodeManager;
	}

	public RoleSpec getRoleSpec()
	{
		return roleSpec;
	}

	public void setRoleSpec(RoleSpec roleSpec)
	{
		this.roleSpec = roleSpec;
	}

	public DetectNewVersionScriptResolver getDetectNewVersionScriptResolver()
	{
		return detectNewVersionScriptResolver;
	}

	public void setDetectNewVersionScriptResolver(DetectNewVersionScriptResolver detectNewVersionScriptResolver)
	{
		this.detectNewVersionScriptResolver = detectNewVersionScriptResolver;
	}

	@RequestMapping
	public String register(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model)
	{
		if (this.applicationProperties.isDisableRegister())
		{
			WebUtils.setOperationMessage(request,
					optMsgFail(request, "registerDisabled"));
			return ERROR_PAGE_URL;
		}

		setFormAction(model, "register", "doRegister");

		User entity = new User();
		setFormModel(model, entity);
		this.detectNewVersionScriptResolver.enableIf(request);
		setUserPasswordStrengthInfo(model);
		
		return "/register";
	}

	@RequestMapping(value = "/doRegister", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> doRegister(HttpServletRequest request, HttpServletResponse response,
			@RequestBody RegisterForm form)
	{
		HttpSession session = request.getSession();

		if (this.applicationProperties.isDisableRegister())
			return optFailResponseEntity(request, HttpStatus.BAD_REQUEST, "registerDisabled");
		
		if (!this.checkCodeManager.isCheckCode(session, CHECK_CODE_MODULE_REGISTER, form.getCheckCode()))
		{
			this.checkCodeManager.removeCheckCode(session, CHECK_CODE_MODULE_REGISTER);
			return optFailResponseEntity(request, HttpStatus.BAD_REQUEST, "checkCodeError");
		}

		User user = form.getUser();

		if (isBlank(user.getName()) || isBlank(user.getPassword()))
			throw new IllegalInputException();

		user.setId(IDUtil.randomIdOnTime20());
		user.setAdmin(false);
		user.setAnonymous(false);
		inflateCreateTime(user);

		if (this.userService.getByNameNoPassword(user.getName()) != null)
			return optFailResponseEntity(request, HttpStatus.BAD_REQUEST, "usernameExists",
					user.getName());

		user.setRoles(this.roleSpec.buildRolesByIds(this.applicationProperties.getDefaultRoleRegister(), true));

		addRegisterUser(user);

		this.checkCodeManager.removeCheckCode(session, CHECK_CODE_MODULE_REGISTER);
		session.setAttribute(SESSION_KEY_REGISTER_USER_NAME, user.getName());

		return optSuccessResponseEntity(request);
	}

	@RequestMapping("/success")
	public String registerSuccess(HttpServletRequest request)
	{
		String successUser = (String) request.getSession().getAttribute(SESSION_KEY_REGISTER_USER_NAME);

		if (isBlank(successUser))
			return "redirect:/register";
		else
			return "/register_success";
	}

	protected void addRegisterUser(User user)
	{
		this.userService.add(user);
	}

	protected void setUserPasswordStrengthInfo(org.springframework.ui.Model model)
	{
		ApplicationProperties properties = getApplicationProperties();

		model.addAttribute("userPasswordStrengthRegex", properties.getUserPasswordStrengthRegex());
		model.addAttribute("userPasswordStrengthTip", properties.getUserPasswordStrengthTip());
	}

	public static class RegisterForm implements ControllerForm
	{
		private static final long serialVersionUID = 1L;

		private User user;

		private String checkCode;

		public RegisterForm()
		{
			super();
		}

		public User getUser()
		{
			return user;
		}

		public void setUser(User user)
		{
			this.user = user;
		}

		public String getCheckCode()
		{
			return checkCode;
		}

		public void setCheckCode(String checkCode)
		{
			this.checkCode = checkCode;
		}
	}
}
