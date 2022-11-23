# README

### Dependencies
To develop locally recommend to install [direnv](https://direnv.net)  to keep the packaging as close as possible to real.

### test
```
chr.sh rell test --source-folder sample/test2 --config config.yml
```
### Repl 
```
chr.sh rell repl --config config.yml -d sample/smallEx -wipe -db -m src.main -e get_addresses -a 1
```

### Start
```
chr.sh start --config config.yml -d sample/smallEx/src -np sample/smallEx/config/node-config.properties
chr.sh start --config config.yml -d sample/smallEx/src
```