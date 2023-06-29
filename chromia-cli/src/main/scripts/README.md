## How to install the Chromia CLI

You need first to unzip the tar in a desired location using; 
```
tar xf chromia-cli.tar.gz 
```

You need then to add it to your path in your shell profile (.zshrc for zsh), this can be done as followed.

```
export PATH=$PATH:<directory path to where you unziped>/bin
```

## Autocomplete
Every time you install a new version of the CLI, the completion file needs to regenerated.

- To get autocomplete, for a bash shell
```
chr --generate-completion=bash > <directory path to where you unziped>/bin/chr-completion.sh
```
- To get autocomplete, for a zsh
```
chr --generate-completion=zsh > <directory path to where you unziped>/bin/chr-completion.sh
```

Then add this line to your shell profile script
```
source <directory path to where you unziped>/bin/chr-completion.sh
```