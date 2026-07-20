# README

The Chromia-cli is a tool intended to make the development cycle and deployment of rell dapps simpler where all the needed functionality needed can be found in one cli.

## Usage 

Use `chr --help` to see available options and parameters. More information found in the [official docs](https://docs.chromia.com).

### Auto completion

To enable auto-completion you need to run the following commands, these needs to run each time a new version of the cli
is installed.

```shell
$ chr --generate-completion=bash > ~/chr-completion.sh
$ source ~/chr-completion.sh
```
## Contributing

Build and test the repo using `mvn install`. 

### Prerequisites

- Docker
- Postgres

Package as a docker image using `-Pdocker`. 
Postgres is needed with `en_US.UTF-8` collation to be able to run the test suite. See [setup guide](https://docs.chromia.com/getting-started/dev-setup/database-setup) for instructions.

We recommend installing [direnv](https://direnv.net) for manual testing, as then you can access the build artifact by calling `chr` in this repo.

### Tests
