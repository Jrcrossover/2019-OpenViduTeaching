docker kill openviduteaching-standalone
docker run --name openviduteaching-standalone -p 4443:4443 -p 8080:8080 -e openvidu.secret=MY_SECRET -e initialDataFile="/initialData.json" -v %cd%\build\initialData.json:/initialData.json --rm diegomzmn/openviduteaching-standalone