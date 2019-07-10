# eventsProjectFrontEnd

Instructions:
1. This is just the front end, the contact with the server part is not yet updated by them - we made our code according to it, so accommodating wouldn't be a problem.
2. We need to use the link given above, it will direct you to the initial events search home. Once we press the submit button, query request with the below mentioned JSON object will be sent.

[
  {"id":"4","date":"10-01-2005","location":"Udhampur, Kashmir","info":"google.com"}, // one object which the shown attributes.
  {"id":"1","date":"1-10-2010","location":"Pangong Lake, Kashmir","info":"google.com"},
  {"id":"7","date":"09-12-2010","location":"Ramban, Kashmir","info":"google.com"},
  {"id":"2","date":"10-10-2010","location":"Kupwara, Kashmir","info":"google.com"},
  {"id":"5","date":"05-05-2014","location":"Katra, Kashmir","info":"google.com"},
  {"id":"6","date":"08-18-2016","location":"Kargil, Kashmir","info":"google.com"},
  {"id":"3","date":"1-10-2019","location":"Leh, Kashmir","info":"fb.com"},
  {"id":"8","date":"04-20-2019","location":"Pulwama, Kashmir","info":"google.com"}
]

3. Once submit button is pressed, new page load with the map, timeline and slider and search text boxes and a search button to the corner.
4. As we can see, with the sliding range the date values changes and so the corresponding time period events in the timeline and the map. 
5. The range of the slider shown is max and min of the data-dates (input dates) we get from the server. 
6. Timeline bar: We can scroll through the timeline bar, and on hovering the events and clicking will take us to the marker on the map and its marker opens up.
6. We can search for new location and new event in the top text boxes shown, once it’s done we can click on submit button - loading new page. In server, we will have to query it again, so for the purpose  of checking we are using below example - 
        [
          {"id":"1","date":"1-10-2010","location":"Doda, Kashmir","info":"google.com"},
          {"id":"2","date":"10-10-2010","location":"Kupwara, Kashmir","info":"google.com"},
          {"id":"3","date":"1-10-2019","location":"Tangmarg, Kashmir","info":"fb.com"},
          {"id":"4","date":"10-01-2005","location":"Udhampur, Kashmir","info":"google.com"},
          {"id":"5","date":"05-05-2014","location":"Beerwah, Kashmir","info":"google.com"}
          ]
