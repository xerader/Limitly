
# Introduction

An application to detect and monitor the screen time usage of the most frequently used applications and use notifications to remove space  

![image](https://github.com/user-attachments/assets/c0965b88-95a1-4e69-8fa3-8dcbc25ac470)


# Needs
- The motivation for this project arises from my attempts to cut my phone usage. The usual methods to do so, such as app timers, proved to be temporary solutions. Through anecdotal experience, I found the impact of the location and time period when I engaged in my phone usage to be significant in my habit. This was substantiated by books I read such as Atomic Habits by James Clear and Dopamine Nation by Dr. Anna Lembke.

- I wish to create an application which would incorporate the things I have learned through my deep dive into “productivity” and combating “addiction”. 

- Phone addiction is a significant issue in the modern day. Most existing methods to restrict phone usage are too restrictive. A large factor to break bad habits, are to replace them, or to be conscious of the times and environment when we are engaging in the habit.  

# Functions
-	Prompts user for the new habits they wish to develop in place of their phone addiction.
	-  This has been completed in terms of the UI being developed. 
	
-	Collects screen time and location-based data of the user. This will include the amount of time spent per application and the WiFi they are connected to.
	- Location/WiFi functionality has to be determined  
	
-	Discovers trends in the data and pinpoint time periods of higher usage of specific applications at specific times and locations. 
	- Specific times done, locations has to be done

-	Notify users when they are using an app during a time in which they frequently use that app. 
	- Done, but rather than +- peaks maybe quartiles would be better? 

-	Stores data daily related to phone usage for future trend analysis.
	- Done
# Form
- The data will be captured through an android application, Limitly, which will be created through android studio. 
- Data analysis will be performed using the Chaquopy framework to make Python API calls to access common data analysis packages. The results of the analysis are stored in the form of .txt files on the local storage of the device. 
- The application has a minimal GUI, with prompts for the user’s input of their desired habits.  
- The notification will be a standard notification pop-up message which informs the user of their typical usage behavior of an application, and a suggestion for another task they can do.


# Process
- Upon launching the application for the first time, the user will be prompted to enter the habits they wish to form.
- The application is constantly running in the background. Data about the user’s screen time usage and location is accessed using the UsageStatMonitor, WifiManager, and FusedLocationProviderClient packages. This information is stored on the device itself in the form of a .csv file. 
- The Chaquopy framework allows python packages to be accessed in an android application. It analyzes the most frequently used applications and the time periods when they are being used. It stores this information in the form of a .csv file.
- The application checks if an application stored in the .csv file is opened during the associated time period and sends a notification prompt informing the user of their usage habits and an alternative suggestion. 
