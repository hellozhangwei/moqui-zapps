import groovy.transform.Field
import org.moqui.impl.screen.ScreenUrlInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.moqui.impl.screen.ScreenDefinition
import org.moqui.impl.screen.ScreenDefinition.SubscreensItem

@Field Logger logger = LoggerFactory.getLogger("GetMenuData")

List menuDataList = sri.getMenuData(sri.screenUrlInfo.extraPathNameList)
// sri.screenUrlInfo.extraPathNameList sample [apps, marble, Catalog, Product, FindProduct]
//logger.info("================sri.screenUrlInfo.extraPathNameList========${sri.screenUrlInfo.extraPathNameList}=")

//ScreenDefinition currentScreenDef = sri.sfi.getScreenDefinition("component://MarbleERP/screen/marble.xml")
//def currentScreenPath = "/apps/marble"

ScreenDefinition currentScreenDef = sri.sfi.getScreenDefinition("component://webroot/screen/webroot/apps.xml")
def currentScreenPath = "/apps"

//ScreenDefinition currentScreenDef =sri.getRootScreenDef()
//def currentScreenPath = ""

def rootScreenMap = [:]
rootScreenMap.name = currentScreenDef.getDefaultMenuName()
rootScreenMap.path = currentScreenPath
rootScreenMap.location = currentScreenDef.location
rootScreenMap.pathWithParams = currentScreenPath
rootScreenMap.title = currentScreenDef.getDefaultMenuName()
rootScreenMap.renderModes = currentScreenDef.renderModes

getSubscreens(rootScreenMap)

//menuDataList[0].subscreens = rootScreenMap.subscreens
if (menuDataList != null) ec.web.sendJsonResponse(rootScreenMap)

def getSubscreens(appsMenu) {

    ScreenDefinition parentScreenDef = sri.sfi.getScreenDefinition(appsMenu.location)
    List<SubscreensItem> subscreensItems = parentScreenDef.getMenuSubscreensItems()

    def subscreens = []

    subscreensItems.each {currentSubscreensItem->
        ScreenDefinition currentScreenDef = sri.sfi.getScreenDefinition(currentSubscreensItem.location)
        String currentScreenPath = "${appsMenu.path}/${currentSubscreensItem.name}"
        ScreenUrlInfo.UrlInstance currentUrlInfo = sri.buildUrl(currentScreenPath)
        ScreenUrlInfo sui = currentUrlInfo.sui
        if(currentScreenDef && currentSubscreensItem.getMenuInclude() && currentUrlInfo?.isPermitted() && !currentScreenDef.hasRequired) {

            String pathWithParams = "/" + sui.preTransitionPathNameList.join("/")
            String parmString = currentUrlInfo.getParameterString()
            if (!parmString.isEmpty()) pathWithParams += ('?' + parmString)

            String image = sui.menuImage
            String imageType = sui.menuImageType
            if (image != null && image.length() > 0 && (imageType == null || imageType.length() == 0 || "url-screen".equals(imageType)))
                image = sri.buildUrl(image).path

            def subscreenMap = [name:currentSubscreensItem.name,
                             title:ec.resource.expand(currentSubscreensItem.menuTitle, ""),
                             path: currentScreenPath,
                             pathWithParams:pathWithParams,
                             image:image,
                             renderModes:sui.targetScreen.renderModes,
                             location:currentScreenDef.location]
            if ("icon".equals(imageType)) subscreenMap.imageType = "icon"
            //def active = ("/" + sri.screenUrlInfo.extraPathNameList.join("/") + "/").indexOf(currentScreenPath + "/")>=0
            //if (active) subscreenMap.active = true
            if (currentUrlInfo.disableLink) subscreenMap.disableLink = true

            subscreens.add(subscreenMap)
            getSubscreens(subscreenMap)
        }

    }

    if (subscreens) {
        appsMenu.subscreens = subscreens
    }

}