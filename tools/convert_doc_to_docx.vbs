Option Explicit

' Usage:
'   cscript //nologo convert_doc_to_docx.vbs "input.doc" "output.docx"
'
' Saves as Office Open XML (.docx). Requires Microsoft Word installed.

Dim inPath, outPath
If WScript.Arguments.Count <> 2 Then
  WScript.Echo "Usage: cscript //nologo convert_doc_to_docx.vbs ""input.doc"" ""output.docx"""
  WScript.Quit 2
End If

inPath = WScript.Arguments.Item(0)
outPath = WScript.Arguments.Item(1)

Dim wordApp, doc
On Error Resume Next
Set wordApp = CreateObject("Word.Application")
If Err.Number <> 0 Then
  WScript.Echo "ERROR: Failed to create Word.Application COM object."
  WScript.Quit 3
End If
On Error GoTo 0

wordApp.Visible = False
wordApp.DisplayAlerts = 0

On Error Resume Next
Set doc = wordApp.Documents.Open(inPath, False, True) ' ConfirmConversions:=False, ReadOnly:=True
If Err.Number <> 0 Then
  WScript.Echo "ERROR: Failed to open input doc: " & inPath
  wordApp.Quit
  WScript.Quit 4
End If
On Error GoTo 0

Const wdFormatXMLDocument = 16

On Error Resume Next
doc.SaveAs2 outPath, wdFormatXMLDocument
If Err.Number <> 0 Then
  WScript.Echo "ERROR: Failed to save docx: " & outPath
  doc.Close False
  wordApp.Quit
  WScript.Quit 5
End If
On Error GoTo 0

doc.Close False
wordApp.Quit

WScript.Echo "OK: " & outPath
WScript.Quit 0

