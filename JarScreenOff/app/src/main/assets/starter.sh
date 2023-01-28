appops set com.tile.screenoff SYSTEM_ALERT_WINDOW allow >/dev/null

file_name="Socket.dex"

origin_path="`dirname $0`/$file_name"

cache_dir="/data/local/tmp"
target_path="$cache_dir/Socket.dex"

if [[ -e $origin_path ]]; then
    cp $origin_path $target_path
    export CLASSPATH="$target_path"
    nohup app_process /system/bin com.tile.screenoff.ShellSocket &
fi

nohup sleep 2;am broadcast -a action.Activate.ScreenOff &