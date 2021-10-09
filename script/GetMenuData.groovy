import groovy.transform.Field
import org.moqui.impl.screen.ScreenUrlInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.moqui.impl.screen.ScreenDefinition
import org.moqui.impl.screen.ScreenDefinition.SubscreensItem

@Field Logger logger = LoggerFactory.getLogger("GetMenuData")

//logger.info("=============sri.screenUrlInfo.extraPathNameList=========" + sri.screenUrlInfo.extraPathNameList)
ScreenDefinition appsScreenDef = sri.sfi.getScreenDefinition("component://webroot/screen/webroot/apps.xml")
def currentScreenDef = appsScreenDef
def currentScreenPath = "/apps"

def subscreensItems = appsScreenDef.getSubscreensItemsSorted()
def pathNameList = sri.screenUrlInfo.extraPathNameList

//pathNameList is like [apps, wesys, dashboard]
//find current active app
if(pathNameList.size()>1) {
    for(SubscreensItem subscreensItem:subscreensItems){
        def appName = pathNameList[1]
        //println ("===============appName===${appName}=========subscreensItem.name===${subscreensItem.name}==")
        if(subscreensItem.name==appName) {
            currentScreenDef = sri.sfi.getScreenDefinition(subscreensItem.location)
            currentScreenPath = "/apps" + "/" + appName
            break
        }
    }
}

def rootScreenMap = [:]
rootScreenMap.name = currentScreenDef.getDefaultMenuName()
rootScreenMap.path = currentScreenPath
rootScreenMap.location = currentScreenDef.location
rootScreenMap.pathWithParams = currentScreenPath
rootScreenMap.title = currentScreenDef.getDefaultMenuName()
rootScreenMap.renderModes = currentScreenDef.renderModes

getSubscreens(rootScreenMap)

//menuDataList[0].subscreens = rootScreenMap.subscreens
ec.web.sendJsonResponse(rootScreenMap)

def getSubscreens(appsMenu) {

    ScreenDefinition parentScreenDef = sri.sfi.getScreenDefinition(appsMenu.location)
    List<SubscreensItem> subscreensItems = parentScreenDef.getMenuSubscreensItems()

    def subscreens = []

    subscreensItems.each {currentSubscreensItem->
        ScreenDefinition currentScreenDef = sri.sfi.getScreenDefinition(currentSubscreensItem.location)
        String currentScreenPath = "${appsMenu.path}/${currentSubscreensItem.name}"
        ScreenUrlInfo.UrlInstance currentUrlInfo = sri.buildUrl(currentScreenPath)
        ScreenUrlInfo sui = currentUrlInfo.sui
        if(currentScreenDef && currentSubscreensItem.getMenuInclude() && currentUrlInfo?.isPermitted()
                && !currentScreenDef.hasRequired && !currentScreenDef.isStandalone()) {

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
                             renderModes:sui.targetScreen?.renderModes,
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