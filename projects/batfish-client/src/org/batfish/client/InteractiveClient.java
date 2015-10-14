package org.batfish.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.batfish.common.BfConsts;
import org.batfish.common.LogHelper;
import org.batfish.common.WorkItem;
import org.batfish.common.CoordConsts.WorkStatusCode;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;


public class InteractiveClient {

//   public static void usage() {
//      System.out.println("Usage: java " + InteractiveClient.class.getName()
//          + " [none/simple/files/dictionary [trigger mask]]");
//      System.out.println("  none - no completors");
//      System.out.println("  simple - a simple completor that comples "
//          + "\"foo\", \"bar\", and \"baz\"");
//      System.out
//          .println("  files - a completor that comples " + "file names");
//      System.out.println("  classes - a completor that comples "
//          + "java class names");
//      System.out
//          .println("  trigger - a special word which causes it to assume "
//              + "the next line is a password");
//      System.out.println("  mask - is the character to print in place of "
//          + "the actual password character");
//      System.out.println("  color - colored prompt and feedback");
//      System.out.println("\n  E.g - java Example simple su '*'\n"
//          + "will use the simple compleator with 'su' triggering\n"
//          + "the use of '*' as a password mask.");
//  }
//
//   public InteractiveClient(String[] args)  {
//      try {
//          Character mask = null;
//          String trigger = null;
//          boolean color = false;
//
//          ConsoleReader reader = new ConsoleReader();
//
//          reader.setPrompt("prompt> ");
//
//          if ((args == null) || (args.length == 0)) {
//              usage();
//
//              return;
//          }
//
//          List<Completer> completors = new LinkedList<Completer>();
//
//          if (args.length > 0) {
//              if (args[0].equals("none")) {
//              }
//              else if (args[0].equals("files")) {
//                  completors.add(new FileNameCompleter());
//              }
//              else if (args[0].equals("simple")) {
//                  completors.add(new StringsCompleter("foo", "bar", "baz"));
//              }
//              else if (args[0].equals("color")) {
//                  color = true;
//                  reader.setPrompt("\u001B[1mfoo\u001B[0m@bar\u001B[32m@baz\u001B[0m> ");
//              }
//              else {
//                  usage();
//
//                  return;
//              }
//          }
//
//          if (args.length == 3) {
//              mask = args[2].charAt(0);
//              trigger = args[1];
//          }
//
//          for (Completer c : completors) {
//              reader.addCompleter(c);
//          }
//
//          String line;
//          PrintWriter out = new PrintWriter(reader.getOutput());
//
//          while ((line = reader.readLine()) != null) {
//              if (color){
//                  out.println("\u001B[33m======>\u001B[0m\"" + line + "\"");
//
//              } else {
//                  out.println("======>\"" + line + "\"");
//              }
//              out.flush();
//
//              // If we input the special word then we will mask
//              // the next line.
//              if ((trigger != null) && (line.compareTo(trigger) == 0)) {
//                  line = reader.readLine("password> ", mask);
//              }
//              if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
//                  break;
//              }
//              if (line.equalsIgnoreCase("cls")) {
//                  reader.clearScreen();
//              }
//
//              String[] words = line.split("\\s+");
//
//              if (words.length > 0)
//                 processCommand(line, out);
//          }
//      }
//      catch (Throwable t) {
//          t.printStackTrace();
//      }
//  }

   private BfCoordWorkHelper _workHelper;
   private BfCoordPoolHelper _poolHelper;
   
   private int _logLevel;
   private String _currentTestrigName = null;
   private String _currentEnvironment = null;

   private PrintWriter _consoleWriter = null;

   public InteractiveClient(String workMgr, String poolMgr)  {
      try {

         _workHelper = new BfCoordWorkHelper(workMgr);
         _poolHelper = new BfCoordPoolHelper(poolMgr);

         _logLevel = LogHelper.LEVEL_OUTPUT;
         
          ConsoleReader reader = new ConsoleReader();
          reader.setPrompt("batfish> ");

          List<Completer> completors = new LinkedList<Completer>();
          completors.add(new StringsCompleter("foo", "bar", "baz"));

          for (Completer c : completors) {
              reader.addCompleter(c);
          }

          String line;

          _consoleWriter = new PrintWriter(reader.getOutput(), true);

          while ((line = reader.readLine()) != null) {

             //skip over empty lines
             if (line.trim().length() == 0)
                continue;

             if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                break;
             }

             if (line.equalsIgnoreCase("cls")) {
                reader.clearScreen();
                continue;
             }

             String[] words = line.split("\\s+");

             if (words.length > 0) {
                if (validCommandUsage(words, _consoleWriter))
                   processCommand(words, _consoleWriter);
             }
          }
      }
      catch (Throwable t) {
          t.printStackTrace();
      }
  }

   private void processCommand(String[] words, PrintWriter out) {

      try {
         switch (words[0]) {
         case "add-worker": {
            boolean result = _poolHelper.addBatfishWorker(words[1]);
            out.println("Result: " + result);
            break;
         }
         case "upload-testrig": {
            boolean result = _workHelper.uploadTestrig(words[1], words[2]);
            out.println("Result: " + result);
            break;
         }
         case "parse-vendor-specific": {
            WorkItem wItem = _workHelper.getWorkItemParseVendorSpecific(words[1]);
            wItem.addRequestParam(BfConsts.ARG_LOG_LEVEL, LogHelper.toString(_logLevel));
            out.println("work-id is " + wItem.getId());
            boolean result = _workHelper.queueWork(wItem);
            out.println("Queuing result: " + result);
            break;
         }
         case "init-testrig": {
        	 String testrigName = words[1];
        	 String testrigFile = words[2];

        	    //upload the testrig
             boolean resultUpload = _workHelper.uploadTestrig(testrigName, testrigFile);
             out.println("Result of uploading testrig: " + resultUpload);

             if (!resultUpload) 
            	 break;

             //vendor specific parsing
             WorkItem wItemPvs = _workHelper.getWorkItemParseVendorSpecific(testrigName);
             boolean resultPvs = execute(wItemPvs, out);
             
             if (!resultPvs)
                break;

             //vendor independent parsing
             WorkItem wItemPvi = _workHelper.getWorkItemParseVendorIndependent(testrigName);
             boolean resultPvi = execute(wItemPvi, out);
             
             if (!resultPvi)
                break;

             //upload a default environment
             boolean resultUploadEnv = _workHelper.uploadEnvironment(testrigName, "default", testrigFile);
             out.println("Result of uploading default environment: " + resultUploadEnv);
             
             //set the name of the current testrig
             _currentTestrigName = testrigName;
             _currentEnvironment = "default";
             out.printf("Set active testrig to %s and environment to %s\n", _currentTestrigName, _currentEnvironment);
             
             break;
          }
         case "set-testrig": {
            String testrigName = words[1];
            String environmentName = words[2];
            
            _currentTestrigName = testrigName;
            _currentEnvironment = environmentName;
            
            out.printf("Set active testrig to %s and environment to %s\n", _currentTestrigName, _currentEnvironment);
            
            break;
         }
         case "generate-dataplane": {

            if (_currentTestrigName == null || _currentEnvironment == null) {
               out.printf("Active testrig name or environment is not set (%s, %s)\n", _currentTestrigName, _currentEnvironment);
               break;
            }
            
            //generate facts
            WorkItem wItemGf = _workHelper.getWorkItemGenerateFacts(_currentTestrigName, _currentEnvironment);
            boolean resultGf = execute(wItemGf, out);

            if (!resultGf) 
               break;

            //generate the data plane
            WorkItem wItemGenDp = _workHelper.getWorkItemGenerateDataPlane(_currentTestrigName, _currentEnvironment);
            boolean resultGenDp = execute(wItemGenDp, out);

            if (!resultGenDp) 
               break;

            //get the data plane
            WorkItem wItemGetDp = _workHelper.getWorkItemGetDataPlane(_currentTestrigName, _currentEnvironment);
            boolean resultGetDp = execute(wItemGetDp, out);

            if (!resultGetDp) 
               break;
                        
            //create z3 encoding
            WorkItem wItemCz3e = _workHelper.getWorkItemCreateZ3Encoding(_currentTestrigName, _currentEnvironment);
            boolean resultCz3e = execute(wItemCz3e, out);

            if (!resultCz3e) 
               break;               
         }
         case "answer": {
            String questionName = words[1];
            String questionFile = words[2];
            
            if (_currentTestrigName == null || _currentEnvironment == null) {
               out.printf("Active testrig name or environment is not set (%s, %s)\n", _currentTestrigName, _currentEnvironment);
               break;
            }
            
            //upload the question
            boolean resultUpload = _workHelper.uploadQuestion(_currentTestrigName, questionName, questionFile);            
            out.println("Result of uploading question: " + resultUpload);

            if (!resultUpload) 
               break;

            //answer the question
            WorkItem wItemAs = _workHelper.getWorkItemAnswerQuestion(_currentTestrigName, _currentEnvironment, questionName);
            execute(wItemAs, out);
            
            break;                        
         }
         case "get-work-status": {
            WorkStatusCode status = _workHelper.getWorkStatus(UUID.fromString(words[1]));
            out.println("Result: " + status);
            break;
         }
         case "get-object": {
            String file = _workHelper.getObject(words[1], words[2]);
            out.println("Result: " + file);
            break;
         }
         case "set-loglevel": {
            String logLevelStr = words[1];
            if (LogHelper.LOG_LEVELS.containsKey(logLevelStr)) {
               _logLevel = LogHelper.LOG_LEVELS.get(logLevelStr);
               writeln("Changed loglevel to " + logLevelStr);
            }
            else {
               write(LogHelper.LEVEL_ERROR, "Undefined loglevel value: %s\n", logLevelStr);
            }
            break;
         }
         default:
            out.println("Unsupported command " + words[0]);
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

  private boolean validCommandUsage(String[] words, PrintWriter out) {
     return true;
  }
  
  private boolean execute(WorkItem wItem, PrintWriter out) throws Exception {

     wItem.addRequestParam(BfConsts.ARG_LOG_LEVEL, LogHelper.toString(_logLevel));
     out.println("work-id is " + wItem.getId());

     boolean queueWorkResult = _workHelper.queueWork(wItem);  
     out.println("Queuing result: " + queueWorkResult);

     if (!queueWorkResult)
        return queueWorkResult;
     
	  WorkStatusCode status = _workHelper.getWorkStatus(wItem.getId());

	  while (status != WorkStatusCode.TERMINATEDABNORMALLY
			  && status != WorkStatusCode.TERMINATEDNORMALLY
			  && status != WorkStatusCode.ASSIGNMENTERROR) {

		  out.printf("status: %s\n", status);

		  Thread.sleep(10 * 1000);

		  status = _workHelper.getWorkStatus(wItem.getId());
	  }

	  out.printf("final status: %s\n", status);

	  // get the results
	  String logFileName = wItem.getId() + ".log";
	  String downloadedFile = _workHelper.getObject(wItem.getTestrigName(), logFileName);
	  
	  if (downloadedFile == null) {
		  out.printf("Failed to get output file %s\n", logFileName);
		  return false;
	  }
	  else {
		  try (BufferedReader br = new BufferedReader(new FileReader(downloadedFile))) {
			  String line = null;
			  while ((line = br.readLine()) != null) {
				  out.println(line);
			  }
		  }	  
	  }  
	  
	  //TODO: remove the log file?
	  
	  return (status == WorkStatusCode.TERMINATEDNORMALLY);
  }
  
  private void writeFinally(String message) {
     _consoleWriter.print(message);
  }
  
  private void write(int msgLogLevel, String message) {
     if (msgLogLevel > _logLevel) {
        writeFinally(message);
     }
  }

  private void writeln(int msgLogLevel, String message) {
     write(msgLogLevel, message + "\n");
  }

  private void writeln(String message) {
     writeFinally(message);
  }
  
  private void write(int msgLogLevel, String format, Object... args) {
     write(msgLogLevel, String.format(format, args));
  }

}