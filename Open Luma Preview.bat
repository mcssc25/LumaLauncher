@echo off
title Luma Launcher PC Preview
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0preview-on-pc.ps1"
if errorlevel 1 pause
