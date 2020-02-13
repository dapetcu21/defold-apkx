# Defold APK Expansion Native Extension

This NE tries to solve the problem of the 100MB APK size limit on Google Play by
using APK Expansion files (OBBs).

## Usage

To use this, begin by enabling LiveUpdate in your game and packaging the LiveUpdate
content as a ZIP file. Upload this to Google Play Console as your OBB.

The general app launch flow should be as such:
1. Check if the OBB file exists on disk at the designated location
2. If it doesn't, start the downloader service. Show its progress to the user.
3. Unzip the OBB file and feed the resources inside to `resource.store_resource()`.

Check out the included example that implements this.

# Patch files

If your game hasn't changed too much since the last update, there's no need
to upload a completely new OBB file. Instead, create a ZIP file with only the
files from your LiveUpdate output that don't already exist in your main OBB file.

Upload that ZIP file as a patch OBB file. This way the users will only download
the content that has changed.

`apkx.zip_open()` accepts opening multiple zip files as if they're a single
unified archive. Use it to open both your main and your patch file at the same
time.

## API

```
local file_path = apkx.get_expansion_apk_file_path(is_main, version)
```

Returns the path to where the OBB should reside.

* `is_main`: `boolean` Are we referring to the main or patch OBB file?
* `version`: `number` The OBB's version code

-----

```
apkx.configure_download_service(options)
```

Configure the downloader service. `options` is a table with the following fields:

* `public_key`: `string` Your Google Play API key for this app. Get it from
`Google Play Console -> Your app -> Services & APIs`
* `salt`: `string` A 20-byte string of cryptographically secure random numbers.
Make sure you change this for every app you use this extension in.
* `on_download_state_change`: `function (self, state)` Called when the download state
changes. `state` is one of the download state constants.
* `on_download_progress`: `function (self, progress)` Called when the download progress
changes. `progress` is a table with the following fields:

  * `current_speed`: `number` Current download speed in bytes / ms.
  * `time_remaining`: `number` Estimated time remaining in ms.
  * `overall_progress`: `number` Current progress. Divide this by `overall_total`.
  * `overall_total`: `number` Total progress.

-----

```
local started = apkx.start_download_service_if_required()
```

Start the downloaded service if it's required.

Returns `true` if the service started or `false` if the OBB files are already
downloaded or if there are no OBB files assigned to the APK in Google Play Console.

-----

```
local zip_handle = apkx.zip_open(zip_filenames)
```

Opens the ZIP archives specified in the `zip_filenames` table for reading. If
multiple archives are specified, this will act as if all the files in any subsequent
archive are copied on top of the first (which may add new files or override
existing ones).

**This does synchronous I/O, so may block and cause frame stutters. Sorry about this.**

-----

```
local contents = apkx.zip_read(zip_handle, file_path)
```

Read the file specified by `file_path` from inside a ZIP. Returns the `contents`
as a Lua string.

**This does synchronous I/O, so may block and cause frame stutters. Sorry about this.**

-----

```
apkx.STATE_IDLE
apkx.STATE_FETCHING_URL
apkx.STATE_CONNECTING
apkx.STATE_DOWNLOADING
apkx.STATE_COMPLETED
apkx.STATE_PAUSED_NETWORK_UNAVAILABLE
apkx.STATE_PAUSED_BY_REQUEST
apkx.STATE_PAUSED_WIFI_DISABLED_NEED_CELLULAR_PERMISSION
apkx.STATE_PAUSED_NEED_CELLULAR_PERMISSION
apkx.STATE_PAUSED_WIFI_DISABLED
apkx.STATE_PAUSED_NEED_WIFI
apkx.STATE_PAUSED_ROAMING
apkx.STATE_PAUSED_NETWORK_SETUP_FAILURE
apkx.STATE_PAUSED_SDCARD_UNAVAILABLE
apkx.STATE_FAILED_UNLICENSED
apkx.STATE_FAILED_FETCHING_URL
apkx.STATE_FAILED_SDCARD_FULL
apkx.STATE_FAILED_CANCELED
apkx.STATE_FAILED
```

Download state constants

-----

