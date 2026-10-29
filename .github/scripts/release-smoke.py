#!/usr/bin/env python3
"""Install a verified APK on a dedicated, synthetic-data-only emulator and exercise R8 UI."""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

apk = str(Path(sys.argv[1]).resolve())
sdk = Path(os.environ["ANDROID_HOME"])
adb = str(sdk / "platform-tools/adb")

def run(*args, timeout=90):
    return subprocess.check_output(args, text=True, stderr=subprocess.STDOUT, timeout=timeout)

width,height=map(int,re.findall(r"(\d+)x(\d+)",run(adb,"shell","wm","size"))[-1])

def swipe_up():
    run(adb,"shell","input","swipe",str(width//2),str(height*4//5),str(width//2),str(height//3),"350")

run(str(sdk / "build-tools/35.0.0/apksigner"), "verify", "--verbose", apk)
badging = run(str(sdk / "build-tools/35.0.0/aapt"), "dump", "badging", apk)
assert "package: name='com.lulema.app' versionCode='2' versionName='0.2.0'" in badging
assert "sdkVersion:'26'" in badging
assert "android.permission.INTERNET" not in badging
assert "android.permission.ACCESS_FINE_LOCATION" not in badging
assert "Success" in run(adb, "install", "-r", apk)
run(adb, "shell", "am", "force-stop", "com.lulema.app")
launch = run(adb, "shell", "am", "start", "-W", "-n", "com.lulema.app/.MainActivity")
assert "Status: ok" in launch

def nodes():
    run(adb, "shell", "uiautomator", "dump", "/data/local/tmp/lulema-smoke.xml", timeout=45)
    return list(ET.fromstring(run(adb, "shell", "cat", "/data/local/tmp/lulema-smoke.xml")).iter("node"))

def find(text, click=False, timeout=90):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        for node in nodes():
            if node.attrib.get("text") == text or node.attrib.get("content-desc") == text:
                if click:
                    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
                    run(adb, "shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
                return node
        time.sleep(0.4)
    raise AssertionError(f"Release smoke UI control unavailable: {text}")

def scroll_find(text, click=False):
    for _ in range(10):
        try:
            return find(text, click, timeout=2)
        except AssertionError:
            swipe_up()
    raise AssertionError(f"Release control was not reachable by scrolling: {text}")

scroll_find("+ 记一次", True)
find("持续时长 / 分钟（可选）")
# Duration is the first numeric edit field. Use a synthetic value and avoid logging record text.
field = next(n for n in nodes() if n.attrib.get("class") == "android.widget.EditText")
x1,y1,x2,y2 = map(int,re.findall(r"\d+",field.attrib["bounds"]))
run(adb,"shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
run(adb,"shell","input","text","15")
run(adb,"shell","input","keyevent","4")
for _ in range(6):
    try:
        find("保存档案", True, timeout=2)
        break
    except AssertionError:
        swipe_up()
else:
    raise AssertionError("Release save control was not reachable with the keyboard dismissed")
find("档案详情")
find("记录已保存，档案柜又多了一个坐标。")
for _ in range(8):
    try:
        find("返回记录", True, timeout=2)
        break
    except AssertionError:
        swipe_up()
else:
    raise AssertionError("Release detail close control unavailable")
for title, expected in [("日历","返回今天"),("统计","历史档案"),("成就","成就中心"),("我的","个人设置"),("首页","周期报告")]:
    find(title, True)
    scroll_find(expected)
find("月报",True)
find("年报",True)
find(f"{run(adb,'shell','date','+%Y').strip()} 年度报告")
assert run(adb,"shell","pidof","com.lulema.app").strip()
print("R8 APK verified: signature, package/version/API 26, offline permissions, install, launch, create/detail, five navigation pages and report switching passed.")
