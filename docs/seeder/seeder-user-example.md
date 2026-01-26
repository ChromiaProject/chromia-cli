# Using the Seeder: Generating Test Data for Rell Projects

This guide explains how to use the seeder commands to generate test data for your Rell projects. The seeder functionality helps you create realistic test data for your local database used by your blockchain during development and testing.

## Overview

The Chromia CLI provides two commands to help you generate test data:

- `chr seeder init` - Generates the initial seeder configuration for your blockchains
- `chr seeder generate` - Creates a Rell module containing the data writing logic based on your configuration

## Initializing Seeder Configuration

To create the initial seeder configuration for your blockchains, run:

```bash
chr seeder init
```

This command will:
- Generate default configuration for every entity that is included in your blockchain configurations
- Configurations will be written at `.chromia/seeder` directory at the root of your project


The seeder configuration is stored in YAML format and defines how your test data should be generated. The configuration includes:
- Entity definitions
- Entity count, which controls how many of a specific entity that should be generated
- Data generation rules for each entity attribute

You can also generate configuration for specific blockchain in chromia.yml:

```bash
chr seeder init --blockchain my-blockchain
```

## Generating the Seeder Module

Once you have configured your seeder, you can generate the Rell module that has the Rell operation that populates your database based on the configuration:

```bash
chr seeder generate
```

This command will:
- Generate data based on the configuration from `.chromia/seeder`
- Generate a Rell module for each blockchain at `src/seeder/seed_[blockchain-name].rell`
- The generated module contains the operation `seed_data`, which when called will write to the database in a transaction
- The generated module also contains contains util functions, and one one function per entity responsible for the writing the data for that entity

:::warning This module should never be part of your blockchain configuration to avoid it being deployed. Do not import this into a non test module
:::


You can also generate seeder modules for specific blockchains:

```bash
chr seeder generate --blockchain my-blockchain
```


## Example

### Default configuration
Given the following entity definitions of a Rell module:
```rell
entity department {
    name;
}

entity employee { 
    name: text;
    age: integer;
    department;
}
```

Running `chr seeder init`, the following default configuration will be generated:

```yaml
department:
  count: 10
  attributes:
    name:
      generator: text

employee:
  count: 10
  attributes:
    name:
      generator: text
    age:
      generator: range
      min: 0
      max: 1000
```
#### References
Note that the referenced attribute of department for employee has no configuration. The references are handled internally during generation, to reference correct `row_ids` of the referenced table (entity).

#### Generators
Each entity attribute type will be given a default generator. In this example we can see that the Rell type `text` gets the `text` generator, and Rell type `integer` get a `range` generator.
The default `text` generator will generate random string. The default `range` generator will generate a random number between the min and max value.

If you wish to use a generator with more realistic data for your use case, please check out the available generators at: TODO LINK

### Populate Database
After running `chr seeder generate` a new Rell module for each blockchain will have been generated in a folder `seeder` within your source folder. This module will contain a operation named `seed_data`, along with some util functions.
For this example the `seed_data` will look like this:

```rell
operation seed_data(batch_size: integer = 1000) {
    val existing_data = map<integer, gtv>();
    seed_main_department(existing_data, batch_size);
    seed_main_employee(existing_data, batch_size);
}
```

Now to populate your database with the configured data for a test case simply call this operation from a transaction:
```rell
function test_populate_database() {
    rell.test.tx(seed_data()).run();
}
```