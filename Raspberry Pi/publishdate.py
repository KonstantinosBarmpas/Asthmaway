#Import neccessary libraries
import paho.mqtt.client as mqtt
import datetime
import time
import json

#attempt to connect to the broker function
def attemptconnect(client):
	try:
		client.connect("test.mosquitto.org",port=8884)
            #only sets connected_flag to true if no exception is thrown
        client.connected_flag=True
	except:
		print("connection failed")

#create a dictionary with the current date and time
def time_to_dict():
	day_time=datetime.datetime.now()
	dict={"full": day_time.ctime()}
	return dict;

#Initialize a client object by calling the mqtt.Client() constructor
client=mqtt.Client()
#Initialize member variable used to check connection with the broker to false
mqtt.Client.connected_flag=False
#Set up the client to successfully publish / subscribe using encryption to the topic:
#IC.embedded/SMARTasses/setup
client.tls_set(ca_certs="mosquitto.org.crt",certfile="client.crt",keyfile="client.key")

#Stays in this loop untill a connection has been established
while (not client.connected_flag): #wait in loop
    print("Attempting to connect to broker")
    attemptconnect(client)
	time.sleep(1)

#Main loop of this script
while(True):
	time_dict=time_to_dict()
    #Publishes using JSON the current date to the topic: IC.embedded/SMARTasses/date
	client.publish("IC.embedded/SMARTasses/date",json.dumps(time_dict))
	time.sleep(1)
