// VERSION: 1.22

metadata {
    definition (name: "Fully Kiosk Browser Controller", namespace: "GvnCampbell", author: "Gavin Campbell", importUrl: "https://raw.githubusercontent.com/GvnCampbell/Hubitat/master/Drivers/FullyKioskBrowserController.groovy") {
		capability "Actuator"
		capability "Alarm"
		capability "AudioVolume"
		capability "Refresh"
		capability "SpeechSynthesis"
		capability "Tone"
		command "bringFullyToFront"
		command "launchAppPackage",["String"]
		command "loadStartURL"
		command "loadURL",["String"]
		command "playSound",["String"]
		command "screenOn"
		command "screenOff"
		command "setScreenBrightness",["Number"]
		command "startScreensaver"
		command "stopScreensaver"
		command "stopSound"
		command "triggerMotion"
    }
	preferences {
		input(name:"serverIP",type:"string",title:"Server IP Address",defaultValue:"",required:true)
		input(name:"serverPort",type:"string",title:"Server Port",defaultValue:"2323",required:true)
		input(name:"serverPassword",type:"string",title:"Server Password",defaultValue:"",required:true)
		input(name:"toneFile",type:"string",title:"Tone Audio File URL",defaultValue:"",required:false)
		input(name:"sirenFile",type:"string",title:"Siren Audio File URL",defaultValue:"",required:false)
		input(name:"sirenVolume",type:"integer",title:"Siren Volume (0-100)",range:[0..100],defaultValue:"100",required:false)
		input(name:"volumeStream",type:"enum",title:"Volume Stream",
			  options:["1":"System","2":"Ring","3":"Music","4":"Alarm","5":"Notification","6":"Bluetooth","7":"System Enforced","8":"DTMF","9":"TTS","10":"Accessibility"],
			  defaultValue:["3"],required:true,multiple:false) // when supported by HE we will set multiple to true
		input(name:"loggingLevel",type:"enum",title:"Logging Level",description:"Set the level of logging.",options:["none","debug","trace","info","warn","error"],defaultValue:"debug",required:true)
    }
}

// *** [ Initialization Methods ] *********************************************
def installed() {
	def logprefix = "[installed] "
    logger(logprefix,"trace!")
	sendEvent([name:"mute",value:"unmuted"])
	sendEvent([name:"alarm",value:"off"])
	sendEvent([name:"volume",value:100])
    initialize()
}
def updated() {
	def logprefix = "[updated] "
	logger(logprefix,"trace!")
	initialize()
}
def initialize() {
	def logprefix = "[initialize] "
    logger(logprefix,"trace!")
}

// *** [ Device Methods ] *****************************************************
def beep() {
	def logprefix = "[beep] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=playSound&url=${java.net.URLEncoder.encode(toneFile, "UTF-8")}")
}
def launchAppPackage(appPackage) {
	def logprefix = "[launchAppPackage] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=startApplication&package=${java.net.URLEncoder.encode(appPackage, "UTF-8")}")
}
def bringFullyToFront() {
	def logprefix = "[bringFullyToFront] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=toForeground")
}
def screenOn() {
	def logprefix = "[screenOn] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=screenOn")
}
def screenOff() {
	def logprefix = "[screenOff] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=screenOff")
}
def setScreenBrightness(value) {
	def logprefix = "[setScreenBrightness] "
	logger(logprefix+"value:${value}","trace")
	sendCommandPost("cmd=setStringSetting&key=screenBrightness&value=${value}")
}
def triggerMotion() {
	def logprefix = "[triggerMotion] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=triggerMotion")
}
def startScreensaver() {
	def logprefix = "[startScreensaver] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=startScreensaver")
}
def stopScreensaver() {
	def logprefix = "[stopScreensaver] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=stopScreensaver")
}
def loadURL(url) {
	def logprefix = "[loadURL] "
	logger(logprefix+"url:${url}","trace")
	sendCommandPost("cmd=loadURL&url=${java.net.URLEncoder.encode(url, "UTF-8")}")
}
def loadStartURL() {
	def logprefix = "[loadStartURL] "
	logger(logprefix,"trace")
	sendCommandPost("cmd=loadStartURL")
}
def speak(text) {
	def logprefix = "[speak] "
	logger(logprefix+"text:${text}","trace")
	def sound = textToSpeech(text)
	logger(logprefix+"sound.uri: ${sound.uri}")
	logger(logprefix+"sound.duration: ${sound.duration}")
	playSound(sound.uri)
}
def setVolume(volumeLevel) {
	def logprefix = "[setVolume] "
	logger(logprefix+"volumeLevel:${volumeLevel}")
	logger(logprefix+"volumeStream:${volumeStream}")
	def vl = volumeLevel.toInteger()
	def vs = volumeStream.toInteger()
	if (vl >= 0 && vl <= 100 && vs >= 1 && vs <= 10) {
		sendCommandPost("cmd=setAudioVolume&level=${vl}&stream=${vs}")
		sendEvent([name:"volume",value:vl])
		state.remove("mute")
		sendEvent([name:"mute",value:"unmuted"])
	} else {
		logger(logprefix+"volumeLevel or volumeStream out of range.")
	}
}
def volumeUp() {
	def logprefix = "[volumeUp] "
	logger(logprefix)
	def newVolume = state.mute ?: device.currentValue("volume")
	if (newVolume) {
		newVolume = newVolume.toInteger() + 10
		newVolume = Math.min(newVolume,100)
		setVolume(newVolume)
	} else {
		logger(logprefix+"No volume currently set.")
	}
}
def volumeDown() {
	def logprefix = "[volumeDown] "
	logger(logprefix)
	def newVolume = state.mute ?: device.currentValue("volume")
	if (newVolume) {
		newVolume = newVolume.toInteger() - 10
		newVolume = Math.max(newVolume,0)
		setVolume(newVolume)
	} else {
		logger(logprefix+"No volume currently set.")
	}
}
def mute() {
	def logprefix = "[mute] "
	logger(logprefix)
	if (!state.mute) {
		setVolume(0)
		state.mute = device.currentValue("volume") ?: 100
		sendEvent([name:"mute",value:"muted"])
		logger(logprefix+"Previous volume saved to state.mute:${state.mute}")
	} else {
		logger(logprefix+"Already muted.")
	}
}
def unmute() {
	def logprefix = "[unmute] "
	logger(logprefix+state.mute)
	if (state.mute) {
		setVolume(state.mute)
	} else {
		logger(logprefix+"Not muted.")
	}
}
def refresh() {
  	def logprefix = "[refresh] "
  	logger logprefix
	sendCommandPost("cmd=deviceInfo")
}
def both() {
	def logprefix = "[both] "
	logger(logprefix)
	sirenStart("both")
}
def strobe() {
	def logprefix = "[strobe] "
	logger(logprefix)
	sirenStart("strobe")
}
def siren() {
	def logprefix = "[siren] "
	logger(logprefix)
	sirenStart("siren")
}
def sirenStart(eventValue) {
	def logprefix = "[sirenStart] "
	logger(logprefix+"sirenFile:${sirenFile}")
	logger(logprefix+"sirenVolume:${sirenVolume}")
	logger(logprefix+"eventValue:${eventValue}")
	if (sirenVolume && sirenFile && eventValue) {
		state.siren = state.mute ?: (device.currentValue("volume") ?: 100)
		logger(logprefix+"Previous volume saved to state.siren:${state.siren}")
		unmute()
		setVolume(sirenVolume)
		sendEvent([name:"alarm",value:eventValue])
		sendCommandPost("cmd=playSound&loop=true&url=${java.net.URLEncoder.encode(sirenFile, "UTF-8")}")
	} else {
		logger(logprefix+"sirenFile,sirenVolume or eventValue not set.")
	}
}
def off() {
	def logprefix = "[off] "
	logger(logprefix+"state.siren:${state.siren}")
	if (state.siren) {
		setVolume(state.siren)
	}
	state.remove("siren")
	sendEvent([name:"alarm",value:"off"])
	sendCommandPost("cmd=stopSound")
}
def playSound(soundFile) {
	def logprefix = "[playSound] "
	logger(logprefix+"soundFile:${soundFile}","trace")
	sendCommandPost("cmd=playSound&url=${java.net.URLEncoder.encode(soundFile, "UTF-8")}")
}
def stopSound() {
	def logprefix = "[stopSound] "
    logger(logprefix,"trace")
	sendCommandPost("cmd=stopSound")
}

// *** [ Communication Methods ] **********************************************
def sendCommandPost(cmdDetails="") {
	def logprefix = "[sendCommandPost] "
	logger(logprefix+"cmdDetails:${cmdDetails}","trace")
	def postParams = [
		uri: "http://${serverIP}:${serverPort}/?type=json&password=${serverPassword}&${cmdDetails}",
		requestContentType: 'application/json',
		contentType: 'application/json'
	]
	logger(logprefix+postParams)
	asynchttpPost("sendCommandCallback", postParams, null)
}
def sendCommandCallback(response, data) {
	def logprefix = "[sendCommandCallback] "
    logger(logprefix+"response.status: ${response.status}","trace")
	if (response?.status == 200) {
		logger(logprefix+"response.data: ${response.data}")
		def jsonData = parseJson(response.data)
		if (jsonData?.ip4 || jsonData?.status == "OK") {
			logger(logprefix+"Updating last activity.")
			sendEvent([name:"refresh"])
		}
	}
}

// *** [ Logger ] *************************************************************
private logger(loggingText,loggingType="debug") {
	def internalLogging = false
	def internalLoggingSize = 500
	if (internalLogging) { if (!state.logger) {	state.logger = [] }	} else { state.logger = [] }

	loggingType = loggingType.toLowerCase()
	def forceLog = false
	if (loggingType.endsWith("!")) {
		forceLog = true
		loggingType = loggingType.substring(0, loggingType.length() - 1)
	}
	def loggingTypeList = ["trace","debug","warn","info","error"]
	if (!loggingTypeList.contains(loggingType)) { loggingType="debug" }
	if ((!loggingLevel||loggingLevel=="none") && loggingType == "error") {
	} else if (forceLog) {
	} else if (loggingLevel == "debug" || (loggingType == "error")) {
	} else if (loggingLevel == "trace" && (loggingType == "trace" || loggingType == "info")) {
	} else if (loggingLevel == "info"  && (loggingType == "info")) {
	} else if (loggingLevel == "warn"  && (loggingType == "warn")) {
	} else { loggingText = null }
	if (loggingText) {
		log."${loggingType}" loggingText
		if (internalLogging) {
			if (state.logger.size() >= internalLoggingSize) { state.logger.pop() }
			state.logger.push("<b>log.${loggingType}:</b>\t${loggingText}")
		}
	}
}
