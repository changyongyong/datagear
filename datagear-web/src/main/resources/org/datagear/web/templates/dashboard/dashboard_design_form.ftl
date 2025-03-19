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
	<@spring.message code='module.dashboard' />
	<#include "../include/html_request_action_suffix.ftl">
	<#include "../include/html_app_name_suffix.ftl">
</title>
</head>
<body class="p-card no-border h-screen m-0 p-1">
<#include "../include/page_obj.ftl">
<div id="${pid}" class="page page-form horizontal h-full page-dashboard-design">
	<form id="${pid}form" class="flex flex-column h-full" :class="{readonly: pm.isReadonlyAction}">
		<div class="page-form-content flex-grow-1 px-2 py-1 flex flex-column overflow-y-auto">
			<div class="field grid flex-grow-0 mb-1">
				<label class="field-label col-12 mb-0">
					{{fm.name}}
					<!--
					<span class="text-color-secondary ml-2" title="<@spring.message code='dashboard.version' />">v<small>{{fm.version}}</small></span>
					-->
				</label>
			</div>
			<div class="field grid mb-0 flex-grow-1 flex flex-column">
		        <div class="field-input col-12 flex-grow-1 flex flex-column">
		        	<div class="grid grid-nogutter flex-grow-1 align-items-stretch">
		        		<div class="col-8 md:col-9 pr-1">
		        			<#include "include/dashboard_design_editor.ftl">
		        		</div>
		        		<div class="col-4 md:col-3 pl-2">
		        			<#include "include/dashboard_design_resource.ftl">
		        		</div>
		        	</div>
		        </div>
			</div>
		</div>
		<div class="page-form-foot flex-grow-0 flex justify-content-center gap-2 pt-2">
			<p-button type="submit" label="<@spring.message code='saveAll' />"></p-button>
			<p-button type="button" label="<@spring.message code='saveAllAndShow' />" @click="onSaveAndShow"></p-button>
		</div>
	</form>
	<#include "include/dashboard_design_resource_forms.ftl">
	<#include "include/dashboard_design_editor_forms.ftl">
	<#include "../include/page_palette.ftl">
	<#include "../include/page_copy_to_clipboard.ftl">
</div>
<#include "../include/page_form.ftl">
<#include "../include/page_simple_form.ftl">
<#include "../include/page_tabview.ftl">
<#include "../include/page_code_editor.ftl">
<#include "../include/page_boolean_options.ftl">
<#include "include/dashboard_code_completions.ftl">
<script>
(function(po)
{
	po.submitUrl = "/dashboard/"+po.submitAction;
	
	po.showUrl = function(name)
	{
		name = (name || "");
		
		var fm = po.vueFormModel();
		return po.concatContextPath("/dv/"+encodeURIComponent(fm.id)+"/"+name);
	};

	po.inSaveAndShowAction = function(val)
	{
		if(val === undefined)
			return (po._inSaveAndShowAction == true);
		
		po._inSaveAndShowAction = val;
	};
	
	po.beforeSubmitForm = function(action)
	{
		var data = action.options.data;
		var templateCount = (data.templates && data.templates.length != null ? data.templates.length : 0);
		
		//隐藏基本信息后无法自动校验名称，所以这里手动校验
		if(!data.name)
		{
			$.tipInfo("<@spring.message code='dashboard.nameRequired' />");
			return false;
		}
		
		data =
		{
			dashboard: data,
			resourceNames: [],
			resourceContents: [],
			resourceIsTemplates: []
		};
		
		var editResInfos = po.getEditResInfos();
		$.each(editResInfos, function(idx, ei)
		{
			data.resourceNames.push(ei.name);
			data.resourceContents.push(ei.content);
			data.resourceIsTemplates.push(ei.isTemplate);
			
			if(ei.isTemplate)
				templateCount += 1;
		});
		
		if(templateCount == 0)
		{
			$.tipWarn("<@spring.message code='dashboard.atLeastOneTemplateRequired' />");
			return false;
		}
		
		action.options.data = data;
		action.options.saveAndShowAction = po.inSaveAndShowAction();
	};
	
	var formModel = $.unescapeHtmlForJson(<@writeJson var=formModel />);
	formModel.analysisProject = (formModel.analysisProject == null ? {} : formModel.analysisProject);
	
	po.setupForm(formModel,
	{
		closeAfterSubmit: false,
		success: function(response)
		{
			po.updateAllResSavedChangeFlags();
			po.updateTemplateList(response.data.templates);
			po.refreshLocalRes();
			
			var options = this;
			if(options.saveAndShowAction)
			{
				var fm = po.vueFormModel();
				var resInfo = po.getCurrentEditResInfo(true);
				
				window.open(po.showUrl(resInfo ? resInfo.name : ""), fm.id);
			}
		}
	});
	
	po.vueMethod(
	{
		onSaveAndShow: function(e)
		{
			try
			{
				po.inSaveAndShowAction(true);
				po.form().submit();
			}
			finally
			{
				po.inSaveAndShowAction(false);
			}
		}
	});
	
	po.setupResourceList();
	po.setupResourceEditor();
	po.setupPalette();
	
	po.vueMounted(function()
	{
		po.showFirstTemplateContent();
	});
})
(${pid});
</script>
<#include "../include/page_vue_mount.ftl">
</body>
</html>