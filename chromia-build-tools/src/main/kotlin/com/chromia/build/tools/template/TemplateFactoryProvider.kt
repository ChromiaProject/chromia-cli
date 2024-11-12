package com.chromia.build.tools.template

object TemplateFactoryProvider {
    fun getFactory(template: TemplateProject): TemplateFactory {
        return when (template) {
            TemplateProject.PLAIN -> PlainTemplateFactory()
            TemplateProject.PLAIN_MULTI -> PlainMultiTemplateFactory()
            TemplateProject.MINIMAL -> MinimalTemplateFactory()
            TemplateProject.PLAIN_LIBRARY -> PlainLibraryTemplateFactory()
            TemplateProject.ASSET_MANAGEMENT -> AssetManagementTemplateFactory()
        }
    }
}
