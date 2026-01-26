# Generators

This document provides a comprehensive catalog of all data generators available in the Rell Toolbox Seeder module. 
These generators create realistic mock data for development, testing, and database seeding purposes.
Each generator produces specialized data values based on its category.

## Using Generators 

To use a specific generator, simply reference its id in your seeder configuration:

```yaml
<entity_name>:
  count: 10
  attributes:
    <attribute_name>:
      generator: <generator_id>
```

## Generator Categories

<details>
<summary>Random Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `random.integer` | Generates a random integer within a configured `min/max` range or any valid long integer. | Long |
| `random.boolean` | Generates a random boolean value (`true` or `false`). | Boolean |
| `random.decimal` | Generates a random decimal number within a configured min/max range or between 0.0 and 1.0. | Double |
| `random.enum` | Generates a random value from a defined Rell enum type. | Integer |
| `random.uuid` | Generates a random UUID. | String |
| `random.text` | Generates a random string with configurable length. | String |
| `random.json` | Generates a simple JSON string. | String |
</details>

<details>
<summary>Person Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `first_name` | Generates a random first name. | String |
| `last_name` | Generates a random last name. | String |
| `name` | Generates a random full name. | String |
| `male_first_name` | Generates a random male first name. | String |
| `female_first_name` | Generates a random female first name. | String |
| `neutral_first_name` | Generates a random gender-neutral first name. | String |
| `name_with_middle` | Generates a random name including a middle name. | String |
</details>

<details>
<summary>Address Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `address.full` | Generates a complete address. | String |
| `address.city` | Generates a random city name. | String |
| `address.country` | Generates a random country name. | String |
| `address.country_code` | Generates a random country code. | String |
| `address.street` | Generates a random street address. | String |
| `address.country_code_long` | Generates a random long-form country code. | String |
| `address.building_number` | Generates a random building number. | String |
| `address.community` | Generates a random community name. | String |
| `address.secondary_address` | Generates a random secondary address. | String |
| `address.postcode` | Generates a random postal code. | String |
| `address.state` | Generates a random state name. | String |
| `address.state_abbr` | Generates a random state abbreviation. | String |
| `address.time_zone` | Generates a random time zone. | String |
| `address.city_with_state` | Generates a random city with state. | String |
| `address.street_name` | Generates a random street name. | String |
| `address.mailbox` | Generates a random mailbox. | String |
</details>

<details>
<summary>Internet Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `email` | Generates a random email address. | String |
| `internet.domain` | Generates a random domain name. | String |
| `internet.private_ipv4_addr` | Generates a random private IPv4 address. | String |
| `internet.public_ipv4_addr` | Generates a random public IPv4 address. | String |
| `internet.ipv4_addr` | Generates a random IPv4 address. | String |
| `internet.ipv6_addr` | Generates a random IPv6 address. | String |
| `internet.mac_addr` | Generates a random MAC address. | String |
| `internet.safe_email` | Generates a random safe email address. | String |
| `internet.slug` | Generates a random URL slug. | String |
| `internet.domain_suffix` | Generates a random domain suffix. | String |
| `internet.user_agent` | Generates a random user agent string. | String |
| `internet.bot_user_agent` | Generates a random bot user agent string. | String |
</details>

<details>
<summary>Company Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `company.name` | Generates a random company name. | String |
| `company.type` | Generates a random company type. | String |
| `company.industry` | Generates a random company industry. | String |
| `company.buzzword` | Generates a random business buzzword. | String |
| `company.catch_phrase` | Generates a random company catch phrase. | String |
</details>

<details>
<summary>Phone Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `phone_number` | Generates a random phone number. | String |
| `cell_phone` | Generates a random cell phone number. | String |
</details>

<details>
<summary>Bank Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `bank.name` | Generates a random bank name. | String |
| `bank.account_number` | Generates a random bank account number. | String |
| `bank.iban` | Generates a random IBAN. | String |
| `bank.bic` | Generates a random BIC/SWIFT code. | String |
</details>

<details>
<summary>Color Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `color.name` | Generates a random color name. | String |
| `color.hex` | Generates a random hex color. | String |
</details>

<details>
<summary>Commerce Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `commerce.department` | Generates a random department name. | String |
| `commerce.product_name` | Generates a random product name. | String |
| `commerce.material` | Generates a random material name. | String |
| `commerce.price` | Generates a random price. | String |
| `commerce.promotion_code` | Generates a random promotion code. | String |
</details>

<details>
<summary>Crypto Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `crypto.md5` | Generates a random MD5 hash. | String |
| `crypto.sha1` | Generates a random SHA-1 hash. | String |
| `crypto.sha256` | Generates a random SHA-256 hash. | String |
</details>

<details>
<summary>Currency Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `currency.code` | Generates a random currency code. | String |
| `currency.name` | Generates a random currency name. | String |
</details>

<details>
<summary>File Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `file.extension` | Generates a random file extension. | String |
| `file.mime_type` | Generates a random MIME type. | String |
| `file.file_name` | Generates a random filename. | String |
| `file.directory_path` | Generates a random directory path. | String |
| `file.file_path` | Generates a random file path. | String |
</details>

<details>
<summary>Gender Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `gender.types` | Generates a random gender type. | String |
| `gender.short` | Generates a random short gender representation. | String |
| `gender.binary_types` | Generates a random binary gender type. | String |
</details>

<details>
<summary>Measurement Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `measurement.height` | Generates a random height measurement. | String |
| `measurement.weight` | Generates a random weight measurement. | String |
| `measurement.volume` | Generates a random volume measurement. | String |
| `measurement.length` | Generates a random length measurement. | String |
| `measurement.area` | Generates a random area measurement. | String |
</details>

<details>
<summary>Money Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `money.amount` | Generates a random monetary amount. | String |
</details>

<details>
<summary>Education Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `edu.university` | Generates a random university name. | String |
| `edu.course` | Generates a random course name. | String |
| `edu.secondary_school` | Generates a random secondary school name. | String |
| `edu.campus` | Generates a random campus building name. | String |
| `edu.subject` | Generates a random academic subject. | String |
| `edu.degree` | Generates a random degree name. | String |
</details>

<details>
<summary>Lorem Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `lorem.word` | Generates a random Lorem Ipsum word. | String |
| `lorem.words` | Generates random Lorem Ipsum words. | String |
| `lorem.sentence` | Generates a random Lorem Ipsum sentence. | String |
| `lorem.sentences` | Generates random Lorem Ipsum sentences. | String |
| `lorem.paragraph` | Generates a random Lorem Ipsum paragraph. | String |
| `lorem.paragraphs` | Generates random Lorem Ipsum paragraphs. | String |
</details>

<details>
<summary>Tech Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `tech.stack` | Generates a random technology stack. | String |
| `tech.programming_language` | Generates a random programming language. | String |
| `tech.os` | Generates a random operating system. | String |
| `tech.browser` | Generates a random web browser name. | String |
</details>

<details>
<summary>Travel Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `travel.airport` | Generates a random airport name. | String |
| `travel.airport_code` | Generates a random airport code. | String |
| `travel.airline` | Generates a random airline name. | String |
| `travel.flight_number` | Generates a random flight number. | String |
| `travel.seat_number` | Generates a random seat number. | String |
</details>

<details>
<summary>Food Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `food.dish` | Generates a random dish name. | String |
| `food.ingredients` | Generates a random food ingredient. | String |
| `food.fruits` | Generates a random fruit name. | String |
| `food.vegetables` | Generates a random vegetable name. | String |
| `food.spices` | Generates a random spice name. | String |
| `food.allergens` | Generates random food allergens. | String |
| `food.description` | Generates a random food description. | String |
| `food.measurement_sizes` | Generates random food measurement sizes. | String |
| `food.measurements` | Generates random food measurements. | String |
| `food.metric_measurements` | Generates random metric food measurements. | String |
| `food.sushi` | Generates a random sushi type. | String |
| `food.ethnic_category` | Generates a random ethnic food category. | String |
</details>

<details>
<summary>Beer Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `beer.name` | Generates a random beer name. | String |
| `beer.style` | Generates a random beer style. | String |
| `beer.hop` | Generates a random beer hop variety. | String |
| `beer.yeast` | Generates a random beer yeast type. | String |
| `beer.malts` | Generates a random beer malt type. | String |
| `beer.brand` | Generates a random beer brand name. | String |
</details>

<details>
<summary>Coffee Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `coffee.blend_name` | Generates a random coffee blend name. | String |
| `coffee.country` | Generates a random coffee origin country. | String |
| `coffee.notes` | Generates random coffee flavor notes. | String |
| `coffee.variety` | Generates a random coffee variety. | String |
| `coffee.region` | Generates a random coffee region. | String |
</details>

<details>
<summary>Dessert Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `dessert.variety` | Generates a random dessert variety. | String |
| `dessert.topping` | Generates a random dessert topping. | String |
| `dessert.flavor` | Generates a random dessert flavor. | String |
| `dessert` | Generates a random dessert. | String |
</details>

<details>
<summary>Tea Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `tea.type` | Generates a random tea type. | String |
| `tea.variety.black` | Generates a random black tea variety. | String |
| `tea.variety.green` | Generates a random green tea variety. | String |
| `tea.variety.white` | Generates a random white tea variety. | String |
| `tea.variety.oolong` | Generates a random oolong tea variety. | String |
| `tea.variety.herbal` | Generates a random herbal tea variety. | String |
</details>

<details>
<summary>Restaurant Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `restaurant.name` | Generates a random restaurant name. | String |
| `restaurant.type` | Generates a random restaurant type. | String |
| `restaurant.description` | Generates a random restaurant description. | String |
| `restaurant.review` | Generates a random restaurant review. | String |
</details>

<details>
<summary>Sports Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `basketball.teams` | Generates a random basketball team. | String |
| `basketball.players` | Generates a random basketball player name. | String |
| `basketball.coaches` | Generates a random basketball coach name. | String |
| `basketball.positions` | Generates a random basketball position. | String |
| `chess.players` | Generates a random chess player name. | String |
| `chess.tournaments` | Generates a random chess tournament name. | String |
| `chess.openings` | Generates a random chess opening. | String |
| `chess.titles` | Generates a random chess title. | String |
| `crossfit.competitions` | Generates a random crossfit competition name. | String |
| `crossfit.male_athletes` | Generates a random male crossfit athlete name. | String |
| `crossfit.female_athletes` | Generates a random female crossfit athlete name. | String |
| `crossfit.movements` | Generates a random crossfit movement. | String |
| `crossfit.girl_workouts` | Generates a random crossfit girl workout. | String |
| `crossfit.hero_workouts` | Generates a random crossfit hero workout. | String |
| `esport.players` | Generates a random esport player name. | String |
| `esport.teams` | Generates a random esport team. | String |
| `esport.events` | Generates a random esport event. | String |
| `esport.leagues` | Generates a random esport league. | String |
| `esport.games` | Generates a random esport game. | String |
| `football.teams` | Generates a random football team. | String |
| `football.players` | Generates a random football player name. | String |
| `football.coaches` | Generates a random football coach name. | String |
| `football.competitions` | Generates a random football competition name. | String |
| `football.positions` | Generates a random football position. | String |
| `mountaineering.mountaineer` | Generates a random mountaineer name. | String |
| `sport.summer_olympics` | Generates a random summer Olympic sport. | String |
| `sport.winter_olympics` | Generates a random winter Olympic sport. | String |
| `sport.summer_paralympics` | Generates a random summer Paralympic sport. | String |
| `sport.winter_paralympics` | Generates a random winter Paralympic sport. | String |
| `sport.ancient_olympics` | Generates a random ancient Olympic sport. | String |
| `sport.unusual` | Generates a random unusual sport. | String |
| `team.name` | Generates a random team name. | String |
| `team.sport` | Generates a random team sport. | String |
| `team.mascot` | Generates a random team mascot. | String |
| `volleyball.team` | Generates a random volleyball team. | String |
| `volleyball.player` | Generates a random volleyball player name. | String |
| `volleyball.coach` | Generates a random volleyball coach name. | String |
| `volleyball.position` | Generates a random volleyball position. | String |
| `volleyball.formation` | Generates a random volleyball formation. | String |
| `world_cup.teams` | Generates a random World Cup team. | String |
| `world_cup.stadiums` | Generates a random World Cup stadium. | String |
| `world_cup.cities` | Generates a random World Cup host city. | String |
</details>

<details>
<summary>Barcode Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `barcode.ean13` | Generates a random EAN-13 barcode. | String |
| `barcode.ean8` | Generates a random EAN-8 barcode. | String |
| `barcode.isbn` | Generates a random ISBN. | String |
</details>

<details>
<summary>Business Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `business.credit_card_number` | Generates a random credit card number. | String |
| `business.credit_card_expiry` | Generates a random credit card expiry date. | String |
| `business.credit_card_type` | Generates a random credit card type. | String |
</details>

<details>
<summary>Creature Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `animal.name` | Generates a random animal name. | String |
| `bird.geo` | Generates random bird geographic information. | String |
| `bird.anatomy` | Generates random bird anatomy terms. | String |
| `bird.colors` | Generates random bird colors. | String |
| `bird.plausible_name` | Generates a plausible bird common name. | String |
| `bird.family_name` | Generates a bird family name. | String |
| `cat.name` | Generates a random cat name. | String |
| `cat.breed` | Generates a random cat breed. | String |
| `cat.registry` | Generates a random cat registry. | String |
| `dog.name` | Generates a random dog name. | String |
| `dog.breed` | Generates a random dog breed. | String |
| `dog.sound` | Generates a random dog sound. | String |
| `dog.meme_phrase` | Generates a random dog meme phrase. | String |
| `dog.coat_length` | Generates a random dog coat length. | String |
| `dog.size` | Generates a random dog size. | String |
| `dog.age` | Generates a random dog age. | String |
| `horse.name` | Generates a random horse name. | String |
| `horse.breed` | Generates a random horse breed. | String |
| `ancient.god` | Generates a random ancient god name. | String |
| `ancient.primordial` | Generates a random ancient primordial entity. | String |
| `ancient.titan` | Generates a random ancient titan name. | String |
| `ancient.hero` | Generates a random ancient hero name. | String |
</details>

<details>
<summary>House Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `house.room` | Generates a random room name. | String |
</details>

<details>
<summary>Industry Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `industry.sector` | Generates a random industry sector. | String |
</details>

<details>
<summary>Stripe Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `stripe.id` | Generates a random Stripe ID. | String |
</details>

<details>
<summary>Subscription Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `subscription.plan` | Generates a random subscription plan. | String |
| `subscription.status` | Generates a random subscription status. | String |
</details>

<details>
<summary>Misc Generators</summary>

| Generator ID | Description | Return Type |
|-----------|-------------|------------|
| `emoji` | Generates a random emoji. | String |
| `date` | Generates a random date. | String |
| `date_time` | Generates a random date and time. | String |
| `timestamp` | Generates a random timestamp. | Long |
</details>