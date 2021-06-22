# CameraControl
Next level camera management.

CameraControl lets you manage access to your cameras depending on which side of the phone they face.

Why? Because I hate it when Snapchat opens up and forces me to see my own depression. But completely disabling all the cameras for Snapchat would render the whole app useless.

This was developed and tested on `LineageOS 17.1`, but should work on any android `>= 8.1`.

### How to install:

1. Install the module on a system with a running XPosed framework
1. Activate the module and select apps you want to limit camera access to.

### How to configure:

- Route 1:
    - Go back to the module list
    - Long press on `CameraControl` and select App Info
    - On the bottom, click on `Advanced`
    - Click on `Additional settings in the app`
- Route 2:
    - Go into device settings
    - Click on apps
    - Somehow tell the device to show all apps
    - Select `CameraControl`
    - On the bottom, click `Advanced`
    - Click on `Additional settings in the app`
- Route 3:
    - Open up an ADB shell
    - Run command `am start-activity com.programminghoch10.cameracontrol/.SettingsActivity`

Now configure away!

___Small intel:__ The configure activity is not accessible from the launcher, 
because LSPosed does not let modules hide their own launcher icons by default anymore. 
Defining a settings activity is an easy way to provide an accessible configuration interface, 
without forcing yet another launcher icon into the list.
One does not have to open the menu often anyways._

### What it looks like
![Screenshot](screenshot.png)