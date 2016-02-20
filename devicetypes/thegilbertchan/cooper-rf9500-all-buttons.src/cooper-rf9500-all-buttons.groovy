metadata {
	definition (name: "Cooper RF9500 (all buttons)", namespace: "thegilbertchan", author: "Gilbert Chan") {
		capability "Actuator"
		capability "Button"
		capability "Configuration"
		capability "Sensor"

		fingerprint deviceId: "0x1200", inClusters: "0x77 0x86 0x75 0x73 0x85 0x72 0xEF", outClusters: "0x26"
	}

	simulator {
		status "button 1 pushed":  "command: 2001, payload: 01"
		status "button 1 held":  "command: 2001, payload: 15"
		status "button 2 pushed":  "command: 2001, payload: 29"
		status "button 2 held":  "command: 2001, payload: 3D"
		status "button 3 pushed":  "command: 2001, payload: 51"
		status "button 3 held":  "command: 2001, payload: 65"
		status "button 4 pushed":  "command: 2001, payload: 79"
		status "button 4 held":  "command: 2001, payload: 8D"
		status "wakeup":  "command: 8407, payload: "
	}
	tiles {
		standardTile("button", "device.button", width: 2, height: 2) {
			state "default", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
		}
		main "button"
		details(["button"])
	}
}

def parse(String description) {
	def results = []
	if (description.startsWith("Err")) {
	    results = createEvent(descriptionText:description, displayed:true)
	} else {
		def cmd = zwave.parse(description, [0x26: 1, 0x2B: 1, 0x80: 1, 0x84: 1])
		if(cmd) results += zwaveEvent(cmd)
		if(!results) results = [ descriptionText: cmd, displayed: false ]
	}
	log.debug("Parsed '$description' to $results")
	return results
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
	def results = [createEvent(descriptionText: "$device.displayName woke up", isStateChange: false)]

    results += configurationCmds().collect{ response(it) }
	results << response(zwave.wakeUpV1.wakeUpNoMoreInformation().format())

	return results
}

// A zwave command for a button press was received convert to button number
def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd) {
	[ descriptionText: "startlevel $cmd"]
	log.info "startlevel $cmd"
	if (cmd.upDown == true) {
		Integer buttonid = 2
	    log.info "button $buttonid pressed"
		checkbuttonEvent(buttonid)
		}
	else if (cmd.upDown == false) {
		Integer buttonid = 3
    	log.info "button $buttonid pressed"
		checkbuttonEvent(buttonid)
	}
}

// The controller likes to repeat the command... ignore repeats
def checkbuttonEvent(buttonid){

	if (state.lastScene == buttonid && (state.repeatCount < 4) && (now() - state.repeatStart < 2000)) {
    	log.debug "Button ${buttonid} repeat ${state.repeatCount}x ${now()}"
        state.repeatCount = state.repeatCount + 1
        createEvent([:])
    }
    else {
    	// If the button was really pressed, store the new scene and handle the button press
        state.lastScene = buttonid
        state.lastLevel = 0
        state.repeatCount = 0
        state.repeatStart = now()

        buttonEvent(buttonid)
    }
}

// Handle a button being pressed
def buttonEvent(button) {
	button = button as Integer
	def result = []
    updateState("currentButton", "$button")   
        // update the device state, recording the button press
        result << createEvent(name: "button", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true)
    result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicGet cmd) {
    [ descriptionText: "$cmd"]
    if(1){
		Integer buttonid = 1
        log.info "button $buttonid pressed"
		checkbuttonEvent(buttonid)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	[ descriptionText: "$cmd"]
    if(1){
		Integer buttonid = 1
        log.info "button $buttonid pressed"
		checkbuttonEvent(buttonid)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd) {
        createEvent([:])
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
	[ descriptionText: "$cmd"]
}

// Update State
def updateState(String name, String value) {
	state[name] = value
	device.updateDataValue(name, value)
}