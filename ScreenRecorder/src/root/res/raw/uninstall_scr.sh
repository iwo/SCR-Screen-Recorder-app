#!/system/bin/sh
#
# This script is intended for emergency de-installation of the SCR audio driver
# During normal operation there should be no need to call this script
#

echo "Uninstalling SCR audio driver"
mount -wo remount /system
if [ $? != 0 ]
then
    echo "Can't mount system in read-write mode. You need to run this script as root!"
    exit 1
fi

ls /system/lib/hw/audio.original_primary.* &> /dev/null
if [ $? != 0 ]
then
    echo "Copies of audio drivers not found. Already uninstalled?"
    exit 1
fi

echo "Removing SCR audio driver and links"
rm /system/lib/hw/audio.primary.default.so
rm /system/lib/hw/scr_audio.conf
rm /system/lib/hw/scr_audio.log

echo "Restoring original audio drivers"
for file in /system/lib/hw/audio.original_primary.*
do
    new_name=${file//\.original_primary\./\.primary\.}
    echo "    $file => $new_name"
    mv $file $new_name
    chmod 644 $new_name
done

if [ -e /system/etc/audio_policy.conf.back ]
then
echo "Restoring original audio policy file"
cp -f /system/etc/audio_policy.conf.back /system/etc/audio_policy.conf
chmod 644 /system/etc/audio_policy.conf
fi

if [ -e /vendor/etc/audio_policy.conf.back ]
then
echo "Restoring original vendor audio policy file"
cp -f /vendor/etc/audio_policy.conf.back /vendor/etc/audio_policy.conf
chmod 644 /vendor/etc/audio_policy.conf
fi

if [ -e /system/etc/media_profiles.xml.back ]
then
echo "Restoring original media profiles file"
cp -f /system/etc/media_profiles.xml.back /system/etc/media_profiles.xml
chmod 644 /system/etc/media_profiles.xml
fi

echo "Done"
