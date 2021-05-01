# bson-json-to-csv

Creates a flat csv files out of a nested bson/json file

## [Download](https://github.com/bilal-fazlani/bson-json-to-csv/releases/latest/download/bson-json-to-csv)


[![GitHub Release Date](https://img.shields.io/github/release-date/bilal-fazlani/bson-json-to-csv?style=for-the-badge)](https://github.com/bilal-fazlani/bson-json-to-csv/releases/latest)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/bilal-fazlani/bson-json-to-csv?color=blue&label=version&sort=semver&style=for-the-badge)](https://github.com/bilal-fazlani/bson-json-to-csv/releases/latest/download/bson-json-to-csv)



## Usage

![help](/images/help.png)

![help](/images/usage.png)

Works with stream of json/bson objects. For example,

```json
{
  "name": "john",
  "location": {
    "city": "mumbai",
    "country": "india"
  }
}
{
  "name": "jane",
  "location": "delhi",
  "tags": ["scala", "java", "big data"]
}
```

Each top level object results into an individual csv row.
The above file will generate a csv with a header and two csv records

```csv
.name,.location.city,.location.country,.tags[1],.location,.tags[0],.tags[2]
john,mumbai,india,,,,
jane,,,java,delhi,scala,big data
```

Version 1.4.0 also adds supports for top level single large array.
All first level items in the array result into an individual csv row.

Example:

```json
[
  {
    "name": "john",
    "location": {
      "city": "mumbai",
      "country": "india"
    }
  },
  {
    "name": "jane",
    "location": "delhi",
    "tags": ["scala", "java", "big data"]
  }
]
```

This will result into same csv as above

The headers are json-paths of corresponding json fields

| .name | .location.city | .location.country | .tags[1] | .location | .tags[0] | .tags[2] |
|-------|----------------|-------------------|----------|-----------|----------|----------|
| john  | mumbai         | india             |          |           |          |          |
| jane  |                |                   | java     | delhi     | scala    | big data |


Notice that as shown in above example, the order of json fields may not reflect exactly same in the csv. 
However the indexes of arrays will be maintained correctly.



