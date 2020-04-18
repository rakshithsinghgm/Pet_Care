# Pet_Care
Sensor and Mobile Computing final project.

Project title:  Cat activity detection
Authors:  Rakshith Singh, Tom Clunie

We would like to perform activity detection for a cat and report statistics related to the activity (distance traveled, calories burned, etc).
We will collect the data using an Android smartwatch (Fossil DW9F2 running Android Wear OS) using the accelerometer, gyroscope, and other sensors as appropriate/available.
If we are able to identify and acquire a more suitable sensor for this application, we will use that sensor instead of the watch.

We will also develop an Android application for a smartphone to receive data from the sensor.
This application will:
1- receive live sensor data for training and labeling
2- create deep learning model(s) in Tensorflow using based on the collected data
3- perform real-time inference of activity type
4- report statistics related to the activities detected (total time in each state, etc)

Collection and labeling of the training dataset will be done manually while observing the cat's activity.
We are also interested in identifying which features are most useful in activity prediction.

COMPLETE TASKS:
- training:
    -- app on the sensor to collect the data and send it to the smartphone
    -- app on the smartphone to receive the data and save to csv
    -- tensorflow and tensorflow-lite models (via Google Colab): ~87% accuracy for 3 classes
- real-time inference using tf-lite on smartphone and on the watch
- 30 minutes of data collection (10m sleeping, 10m inactive/notmoving, 10m active/moving)

INCOMPLETE TASKS:
- user app on the phone to collect data from the watch and display it:
    -- app performs real-time inference, cumulative summary of pet activity, bkgd notifications, energy efficient
- watch app:
    -- collect data in energy-efficient manner, perform inference, send data to user smartphone app on demand
- (?) feature analysis