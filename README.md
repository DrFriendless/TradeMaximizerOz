TradeMaximizerOz
================

Australian version of TradeMaximizer, which integrates with boardgamegeek.com

Program instructions
--------------------

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

  
Development setup
-----------------

* Install JDK (Java Development Kit), e.g. version 8 for 32-bit
* Install IntelliJ IDEA, e.g. version 2016.3.5
* Run IntelliJ IDEA
* Open trademaximiser.ipr file
* May need to 'Setup SDK' by selecting the JDK directory, e.g. C:\Program Files (x86)\Java\jdk1.8.0_121 
* Use Build > Build Project to compile
* Then Build > Build Artifacts to create the tm.jar file
  (the output may end up somewhere like out\artifacts\tm_jar, so modify tm.bat accordingly)

    
Examples of running program
---------------------------

These are in the Examples folder.

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

  
Running a maths trade
=====================

Starting instructions
---------------------

1. Test the TradeMaximizerOz software, to make sure you know how to use it.

2. Write up instructions for the trade, including rules and important dates (see previous trades for examples)

   * Initially the description can include a link to the forum where you will post the discussion topic (i.e. State/City forum). This can be updated after creating the discussion topic.

3. Create a Geek List to hold the items. 

   * Make the title clear, e.g. "City (Australia) Month Year maths trade"
   
   * Description should include your instructions
   
   * Domains = BoardGameGeek
   
   * (Default) Private = No, Public Additions = Yes, Comments = Yes
   
   * Trade/Auction List = Yes
   
   * (Default) Sort = Normal
   
   e.g. https://www.boardgamegeek.com/geeklist/200667/australia-wide-cancon-2016-ship-shape-maths-trade

4. Create a discussion topic in the relevant forum (for the State/City). Point the discussion to the Geek List.

5. Update the description to point to the actual forum topic.

6. Add the trade list to the Australian Trade meta-list. Make sure to remove it after the trade is complete:

    https://www.boardgamegeek.com/geeklist/61076/australian-trade-and-sale-meta-list

7. Post an announcement message in the Australian Trade subscription thread:

   https://www.boardgamegeek.com/thread/483458/australian-ultimate-trade-subscription-thread-and


Guides
------

https://www.boardgamegeek.com/wiki/page/Aussie_Maths_Trade

https://www.boardgamegeek.com/thread/154123/math-trade-moderators-guide-last-update-july-27-20
  

Example of trade lists (CanCon 2016)
------------------------------------

https://www.boardgamegeek.com/geeklist/200667/australia-wide-cancon-2016-ship-shape-maths-trade

https://www.boardgamegeek.com/thread/1484829/australia-wide-cancon-2016-ship-shape-maths-trade

