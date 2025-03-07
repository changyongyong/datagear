<#--
 *
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
 *
-->
<#include "../include/page_import.ftl">
<#include "../include/html_doctype.ftl">
<html>
<head>
<#include "../include/html_head.ftl">
<title>
	<@spring.message code='module.user' />
	<#include "../include/html_request_action_suffix.ftl">
	<#include "../include/html_app_name_suffix.ftl">
</title>
</head>
<body class="p-card no-border">
<#include "../include/page_obj.ftl">
<div id="${pid}" class="page page-form horizontal">
	<form id="${pid}form" class="flex flex-column" :class="{readonly: pm.isReadonlyAction}">
		<div class="page-form-content flex-grow-1 px-2 py-1 overflow-y-auto">
			<div class="field grid">
				<label for="${pid}name" class="field-label col-12 mb-2 md:col-3 md:mb-0">
					<@spring.message code='username' />
				</label>
		        <div class="field-input col-12 md:col-9">
		        	<p-inputtext id="${pid}name" v-model="fm.name" type="text" class="input w-full"
		        		name="name" required maxlength="50" :autofocus="!pm.disableEditName" :readonly="pm.disableEditName">
		        	</p-inputtext>
		        </div>
			</div>
			<div class="field grid" v-if="pm.enablePassword">
				<label for="${pid}password" class="field-label col-12 mb-2 md:col-3 md:mb-0">
					<@spring.message code='password' />
				</label>
		        <div class="field-input col-12 md:col-9">
		        	<p-password id="${pid}password" v-model="fm.password" class="input w-full"
		        		input-class="w-full" toggle-mask :feedback="false" :required="pm.enablePassword"
		        		:pt="{input:{name:'password',maxlength:'50',autocomplete:'new-password'}}">
		        	</p-password>
		        	<div class="desc text-color-secondary" v-if="pm.userPasswordStrengthTip != ''">
		        		<small>{{pm.userPasswordStrengthTip}}</small>
		        	</div>
		        </div>
			</div>
			<div class="field grid" v-if="pm.enablePassword">
				<label for="${pid}confirmPassword" class="field-label col-12 mb-2 md:col-3 md:mb-0">
					<@spring.message code='confirmPassword' />
				</label>
		        <div class="field-input col-12 md:col-9">
		        	<p-password id="${pid}confirmPassword" v-model="fm.confirmPassword" class="input w-full"
		        		input-class="w-full" toggle-mask :feedback="false" :required="pm.enablePassword"
		        		:pt="{input:{name:'confirmPassword',maxlength:'50',autocomplete:'new-password'}}">
		        	</p-password>
		        </div>
			</div>
			<div class="field grid">
				<label for="${pid}realName" class="field-label col-12 mb-2 md:col-3 md:mb-0">
					<@spring.message code='realName' />
				</label>
		        <div class="field-input col-12 md:col-9">
		        	<p-inputtext id="${pid}realName" v-model="fm.realName" type="text" class="input w-full"
		        		name="realName" maxlength="50" :autofocus="pm.disableEditName && !pm.enablePassword">
		        	</p-inputtext>
		        </div>
			</div>
			<div class="field grid" v-if="!pm.disableRoles">
				<label for="${pid}roles" class="field-label col-12 mb-2 md:col-3 md:mb-0">
					<@spring.message code='module.role' />
				</label>
		        <div class="field-input col-12 md:col-9">
		        	<div id="${pid}roles" class="input p-component p-inputtext w-full overflow-auto" style="height:6rem;">
		        		<p-chip v-for="role in fm.roles" :key="role.id" :label="role.name" class="mb-2" :removable="!pm.isReadonlyAction" @remove="onRemoveRole($event, role.id)"></p-chip>
		        	</div>
		        	<div>
			        	<p-button type="button" label="<@spring.message code='select' />"
			        		@click="onSelectRole" class="p-button-secondary mt-1"
			        		v-if="!pm.isReadonlyAction">
			        	</p-button>
		        	</div>
		        </div>
			</div>
			<div class="field grid" v-if="pm.isReadonlyAction">
				<label for="${pid}createTime" class="field-label col-12 mb-2 md:col-3 md:mb-0">
					<@spring.message code='createTime' />
				</label>
		        <div class="field-input col-12 md:col-9">
		        	<p-inputtext id="${pid}createTime" v-model="fm.createTime" type="text" class="input w-full"
		        		name="createTime" readonly="readonly">
		        	</p-inputtext>
		        </div>
			</div>
		</div>
		<div class="page-form-foot flex-grow-0 flex justify-content-center gap-2 pt-2">
			<p-button type="submit" label="<@spring.message code='save' />"></p-button>
		</div>
	</form>
</div>
<#include "../include/page_form.ftl">
<script>
(function(po)
{
	po.submitUrl = "/user/"+po.submitAction;
	po.disableRoles = ("${(disableRoles!false)?string('true', 'false')}"  == "true");
	po.enablePassword = ("${(enablePassword!false)?string('true', 'false')}"  == "true");
	po.disableEditName = ("${(disableEditName!false)?string('true', 'false')}"  == "true");
	po.userPasswordStrengthTip = "${userPasswordStrengthTip}";

	po.beforeSubmitForm = function(action)
	{
		var data = action.options.data;
		data.confirmPassword = undefined;
	};
	
	po.vuePageModel(
	{
		disableRoles: po.disableRoles,
		enablePassword: po.enablePassword,
		disableEditName: po.disableEditName,
		userPasswordStrengthTip: po.userPasswordStrengthTip
	});
	
	var formModel = $.unescapeHtmlForJson(<@writeJson var=formModel />);
	po.setupForm(formModel, {}, function()
	{
		var options = {};
		
		if(po.enablePassword)
		{
			options =
			{
				rules:
				{
					"password":
					{
						"pattern" : ${userPasswordStrengthRegex}
					},
					"confirmPassword":
					{
						"equalTo" : po.elementOfName("password")
					}
				}
			};
		}
		
		return options;
	});
	
	po.vueMethod(
	{
		onRemoveRole: function(e, roleId)
		{
			var fm = po.vueFormModel();
			var roles = (fm.roles || []);
			$.removeById(roles, roleId);
		},
		
		onSelectRole: function()
		{
			po.handleOpenSelectAction("/role/select?multiple", function(roles)
			{
				var fm = po.vueFormModel();
				var fmRoles = (fm.roles || (fm.roles = []));
				$.addById(fmRoles, roles);
			});
		}
	});
})
(${pid});
</script>
<#include "../include/page_vue_mount.ftl">
</body>
</html>