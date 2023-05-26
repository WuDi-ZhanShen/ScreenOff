pm grant com.tile.screenoff android.permission.WRITE_SECURE_SETTINGS

file_name="ScreenController.dex"

origin_path="$(dirname "$0")/$file_name"

cache_dir="/data/local/tmp"
target_path="$cache_dir/ScreenController.dex"

if [[ -e $origin_path ]]; then
    cp -rf "$origin_path" $target_path
    export CLASSPATH="$target_path"
    nohup app_process /system/bin com.tile.screenoff.ScreenController > /dev/null 2>&1 &
fi