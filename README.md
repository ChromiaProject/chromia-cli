# README

### Dependencies
To develop locally recommend to install [direnv](https://direnv.net)  to keep the packaging as close as possible to real.

### test
```
chr.sh test -d sample/test2 --settings config.yml
```
### Repl 
```
chr.sh repl --settings config.yml -d sample/smallEx --wipe -db -m src.main2 -e get_addresses -a 1
chr.sh repl --settings config.yml -d sample/smallEx -m src.main2 -e addition
```

### Start
```
chr.sh start --settings config.yml -d sample/smallEx/src -np sample/smallEx/config/node-config.properties
chr.sh start --settings config.yml -d sample/smallEx/src
```

### Compile
```
chr.sh compile --settings config.yml -d sample/smallEx/src
chr.sh compile --settings config.yml -d sample/smallEx/src -o sample/smallEx/src/build
```

### Deploy
```

```