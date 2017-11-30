# sensor_plot
All soft sensor plot

Todo:

1. Set up FileSystemWatcher on the Windows machine. Every time a new CSV file is generated, FileSystemWatcher should invoke the pusher python script. In this repo, the monitor.sh script is the FileSystemWatcher equivalent, and test.py is the pusher script. If PowerShell becomes a pain, we can look into just using a python script: https://pypi.python.org/pypi/watcher/ seems like what we need.

2. Change Android pusher listener so that it also listens to the IP address of the server. Right now, it's configured to request CSV file from localhost. Obviously this wouldn't work on a real life scenario.

3. Test and verify the plots on the Android device is correct. The CSV files used during development all have similar dataset, so plots were not very interesting. 

My report so far: https://docs.google.com/document/d/1MWVBqfvC79qlSejPfLbG3t7aJTfqiKlix54ffyuflVk/edit?usp=sharing
