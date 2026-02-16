Option Explicit

' Usage:
'   cscript //nologo convert_docs_in_folder.vbs "source_folder" "target_folder" "mapping_txt"
'
' Converts all *.doc (legacy Word) in source_folder to *.docx in target_folder.
' Output names are ASCII: converted_001.docx, converted_002.docx, ...
' Writes mapping file (UTF-16LE) with: output_filename<TAB>original_fullname
'
' Requires Microsoft Word installed.

If WScript.Arguments.Count <> 3 Then
  WScript.Echo "Usage: cscript //nologo convert_docs_in_folder.vbs ""source_folder"" ""target_folder"" ""mapping_txt"""
  WScript.Quit 2
End If

Dim sourceFolderPath, targetFolderPath, mappingPath
sourceFolderPath = WScript.Arguments.Item(0)
targetFolderPath = WScript.Arguments.Item(1)
mappingPath = WScript.Arguments.Item(2)

Dim fso
Set fso = CreateObject("Scripting.FileSystemObject")

If Not fso.FolderExists(sourceFolderPath) Then
  WScript.Echo "ERROR: Source folder does not exist: " & sourceFolderPath
  WScript.Quit 3
End If

If Not fso.FolderExists(targetFolderPath) Then
  WScript.Echo "ERROR: Target folder does not exist: " & targetFolderPath
  WScript.Quit 4
End If

Dim wordApp
On Error Resume Next
Set wordApp = CreateObject("Word.Application")
If Err.Number <> 0 Then
  WScript.Echo "ERROR: Failed to create Word.Application COM object."
  WScript.Quit 5
End If
On Error GoTo 0

wordApp.Visible = False
wordApp.DisplayAlerts = 0

Const wdFormatXMLDocument = 16

Dim folder, file, idx
Set folder = fso.GetFolder(sourceFolderPath)

Dim outLines()
ReDim outLines(0)
idx = 0

For Each file In folder.Files
  Dim ext
  ext = LCase(fso.GetExtensionName(file.Name))
  If ext = "doc" Then
    idx = idx + 1
    Dim outName, outPath
    outName = "converted_" & Right("000" & CStr(idx), 3) & ".docx"
    outPath = fso.BuildPath(targetFolderPath, outName)

    Dim doc
    On Error Resume Next
    Set doc = wordApp.Documents.Open(file.Path, False, True)
    If Err.Number <> 0 Then
      ' Skip file but keep going
      Err.Clear
    Else
      doc.SaveAs2 outPath, wdFormatXMLDocument
      doc.Close False

      ' Save mapping line: output<TAB>original
      Dim line
      line = outName & vbTab & file.Name
      Dim n
      n = UBound(outLines)
      If n = 0 And outLines(0) = "" Then
        outLines(0) = line
      Else
        ReDim Preserve outLines(n + 1)
        outLines(n + 1) = line
      End If
    End If
    On Error GoTo 0
  End If
Next

wordApp.Quit

' Write mapping as Unicode text
Dim ts, i
Set ts = fso.OpenTextFile(mappingPath, 2, True, -1) ' ForWriting, create, Unicode
For i = 0 To UBound(outLines)
  If outLines(i) <> "" Then
    ts.WriteLine outLines(i)
  End If
Next
ts.Close

WScript.Echo "OK: Converted " & CStr(idx) & " .doc files. Mapping: " & mappingPath
WScript.Quit 0

