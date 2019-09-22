# Programming Assignment: SQL Evaluator

Write a command-line program that evaluates simple SQL queries.

You don't have to write an SQL parser.  We've provided a tool (`sql-to-json`) that converts [our subset of SQL](#sql-syntax) into a [JSON-based format](#json-formatted-sql) for your program to load.

```
$ YOUR-PROGRAM <table-folder> <sql-json-file> <output-file>
```

Your program should:
1. Load the [JSON-formatted SQL](#json-formatted-sql) file.
2. Load the [JSON-formatted tables](#table-json) referenced by the query.  For example, the table "countries" should be loaded from "\<table-folder\>/countries.table.json".
3. If there are errors in the query, write an error message to the output file and exit.
4. If there are no errors, evaluate the query and write the result table to the output file.

You can assume the SQL JSON and Table JSON files are syntactically valid.  However, you should detect logical errors in the queries, such as:
- References to column names or table names that don't exist.
- Ambiguous column references (the column name exists in multiple tables).
- Use of a comparison operator on incompatible types (string vs integer).

You should perform the evaluation entirely in memory, using the standard data structures provided by your programming language.
- Avoid libraries that already implement a relational database, or a large subset of one; for example, do not use SQLite, Apache Derby, Pandas dataframes, etc.
- You can use external libraries to help with reading/writing JSON.
- If you're writing in Java or Go, we've provided code to handle the JSON parsing part; see the "starter-java/" and "starter-go/" folders.

In addition to your program, you should provide a readme file.  Be sure to read *evaluation criteria* below!

## How to evaluate an SQL query

A simple way to evaluate SQL:
1. Construct the cross-product of all the tables in the `FROM`.
2. Filter things down by checking the `WHERE` conditions on each row of the cross-product.
3. Pull out the columns referenced in the `SELECT`.

Processing the cross-product can be an expensive operation.  For some queries (like our "simple-\*.sql" examples), it might be necessary.  However, most real-world queries (like our "cities-\*.sql" examples) can be evaluated more efficiently.

We recommend starting with the cross-product approach.  Once you have it working, consider ways to improve performance for real-world queries.

## Examples

In the "examples/" folder:
- Table data is in the ".table.json" files
- Queries are in the ".sql" files.  The JSON-formatted versions of each query is in the ".sql.json" files -- these are the files your program will read.
- The expected output for each query is in the ".expected" files.

To start, skim over the "\*.sql" files to get an idea of what queries look like.  We recommend starting with the "error-\*.sql" and "simple-\*.sql" examples, which cover the fundamentals.

Make sure you have Python 2.7+ or 3.2+ installed (`python --version`) to run our `sql-to-json` and `check` tools.

To run your program against the "simple-1.sql" example query:
```
$ ./sql-to-json examples/simple-1.sql  # Writes to "examples/simple-1.sql.json"
$ YOUR-PROGRAM examples examples/simple-1.sql.json examples/simple-1.out
$ diff examples/simple-1.expected examples/simple-1.out
```

You can use the provided `check` tool to do all those steps for you:
```
$ ./check YOUR-PROGRAM -- <table-folder> <sql-files...>
```

For example, if your program is run via `python3 sql_evaluator.py`:
```
$ ./check python3 sql_evaluator.py -- examples examples/simple-1.sql
```

To run against all the example queries:
```
$ ./check python3 sql_evaluator.py -- examples examples/*.sql
```

## Evaluation Criteria

- **Above all else**, we're looking for clean code: correct, easy to understand, and easy to maintain.
- The code should be somewhat efficient.  Don't worry about profiling and measuring microseconds, but don't just throw away CPU or memory doing redundant work.
- We'd like to see some improvement over the basic cross-product technique.  Though some queries will require processing the full cross-product, most real-world queries can be evaluated more efficiently.
    - Assume that the normal use case involves loading the tables once and then evaluating many different queries.  (Your program doesn't _actually_ do this, but use this model for evaluating performance.)
    - Assume that tables can be large (e.g. 100k rows) but everything still fits comfortably in memory.

To allow focusing on the above priorities, we're explicitly excluding testing from the evaluation criteria.  Feel free to write the tests necessary to gain confidence in your code, but we won't be looking at them.

Don't worry about extensibility beyond the requirements given here.  For example, don't worry about handling other SQL features, other SQL data types, non-read-only tables, etc.

When you submit your code, **you must include a readme text file** with:
- How long you spent on the assignment.
- Instructions on how to run your code.
- A brief explanation of your design choices.
- If you had five more hours to improve performance, what would you do?

## File Formats

### Table JSON

Example: [examples/cities.table.json](examples/cities.table.json)

Each ".table.json" file is a JSON array.  The first element is a list of column definitions.  Each column definition is a pair where the first element is the column name and the second element is the column type (either "str" or "int").

The rest of the elements are the table's rows.  Each cell value will be either a string or an integer, depending on the columnn type.

### SQL Syntax

(You don't have to write a parser for this syntax.  The included Python program `sql-to-json` will convert SQL to a JSON-formatted equivalent.)

```
Query =
    "SELECT" Selector ( "," Selector )*
    "FROM" TableDecl ( "," TableDecl )*
    ( "WHERE" Comparison ( "AND" Comparison )* )?

Selector = ColumnRef ( "AS" <identifier> )?

TableDecl = <identifier> ( "AS" <identifier> )?

Comparison = Term ( "=" | "!=" | ">" | ">=" | "<" | "<=" ) Term

Term = ColumnRef | <string-literal> | <integer-literal>

ColumnRef = <identifier> ( "." <identifier> )?
```

Comments start with "--" and go to the end of the line.

Joins are performed using [implicit cross-join notation](https://en.wikipedia.org/wiki/Join_(SQL)#Inner_join).

### JSON-formatted SQL

Example: [examples/cities-1.sql.json](examples/cities-1.sql.json) (converted from [examples/cities-1.sql](examples/cities-1.sql))

```
Query = {
    select: Array<Selector>  // non-empty array
    from: Array<TableDecl>  // non-empty array
    where: Array<Comparison>
}

Selector = {
    column: ColumnRef
    as: string  // automatically derived from 'column' if there's no explicit "AS"
}

TableDecl = {
    source: string  // the file to load (without the ".table.json" extension)
    as: string  // automatically derived from 'source' if there's no explicit "AS"
}

Comparison = {
    op: "=" | "!=" | ">" | ">=" | "<" | "<="
    left: Term
    right: Term
}

Term = {column: ColumnRef} | {literal: int | string}

ColumnRef = {
    name: string
    table: string | null  // non-null if the reference is fully-qualified ("table1.column2")
}
```
