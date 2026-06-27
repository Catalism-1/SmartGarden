@echo off
echo Use the IPv4 Address of the WiFi adapter for Android and ESP8266.
ipconfig | findstr /R /C:"IPv4"
