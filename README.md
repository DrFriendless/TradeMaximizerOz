TradeMaximizerOz
================

Australian version of TradeMaximizer, which integrates with boardgamegeek.com

Instructions
------------

See instructions.html for instructions for the original TradeMaximizer,
however this version works slightly differently.

* The application takes a parameter with the name of the wantlist input file,
  rather than taking it from standard input.
* Similarly, the output files are hardcoded to result.txt and result.html,
  rather than standard out.
* The wantlist file must contain the following options:
  * GEEKLIST: ID of the GeekList on boardgamegeek.com that contains the items
  * CODE-DIGITS: Number of digits in item codes (the digits match the list item index)
  * CODE-LETTERS: Number of letters in item codes, for a mnemonic code

Examples
--------

### Example 1 ###

The examples directory contains two example files, and a batch file.

After compiling the project (with IntelliJ IDEA Community Edition) and building the tm.jar artifact,
the batch file can be used to run the examples, e.g.
 .\tm.bat .\input-example-1.txt

This will create the following output files:
 geeklist_140281_page{1}.xml - downloaded copy of the boardgamegeek.com Geek List
 result.txt - results of the trade, in the original text format
 result.html - results in nicely formatted HTML
 errors.txt - any errors that occurred

In the case of input-example-1.txt, the Geek List referenced does not match the
simple input file, so there will be lots of errors for missing data.

In fact, input-example-1.txt is the same input as the first trade in the
original instructions, README-Original.txt, and has the same results (in result.txt)

In result.html, you will see the enhanced HTML results, which include all the users
from the referenced Geek List as well.


### Example 2 ###

For an example from an actual past trade, run:
 .\tm.bat .\input-amiguero6.txt

This will download a different Geek List, and has more complicated results.

[2016-01-26] Currently codes are not working because the returned XML does not include
comments, even though specified in the URL, e.g.:
  http://boardgamegeek.com/xmlapi/geeklist/172859?comments=1





