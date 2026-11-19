using System;
using System.Text;
using System.Runtime.InteropServices;
public static class BreadConsole
{
    [StructLayout(LayoutKind.Sequential)] public struct Coord
    {
        public short X,Y;
        public Coord(short x,short y)
        {
            X=x;
            Y=y;
        }
    }
    [StructLayout(LayoutKind.Sequential)] public struct Rect
    {
        public short Left,Top,Right,Bottom;
    }
    [StructLayout(LayoutKind.Sequential)] public struct Info
    {
        public Coord Size,Cursor;
        public short Attributes;
        public Rect Window;
        public Coord Maximum;
    }
    [StructLayout(LayoutKind.Explicit,Size=20,CharSet=CharSet.Unicode)] public struct Key
    {
        [FieldOffset(0)] public short Type;
        [FieldOffset(4)] public int Down;
        [FieldOffset(8)] public short Repeat;
        [FieldOffset(10)] public short VirtualKey;
        [FieldOffset(14)] public char Character;
    }
    [StructLayout(LayoutKind.Sequential)] public struct WindowRect
    {
        public int Left,Top,Right,Bottom;
    }
    [DllImport("kernel32.dll")] public static extern IntPtr GetStdHandle(int value);
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode)] static extern IntPtr CreateFile(string name,uint access,uint share,IntPtr security,uint create,uint flags,IntPtr template);
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr handle);
    [DllImport("kernel32.dll")] static extern bool SetStdHandle(int kind,IntPtr handle);
    [DllImport("kernel32.dll")] static extern bool SetHandleInformation(IntPtr handle,uint mask,uint flags);
    public static void BindConsoleInput() {
        var input=CreateFile("CONIN$",0xc0000000,3,IntPtr.Zero,3,0,IntPtr.Zero);
        if(input==new IntPtr(-1)||!SetHandleInformation(input,1,1)||!SetStdHandle(-10,input))throw new Exception("Native input binding failed");
    }
    static IntPtr ActiveOutput() { return CreateFile("CONOUT$",0x80000000,3,IntPtr.Zero,3,0,IntPtr.Zero); }
    [DllImport("kernel32.dll")] public static extern bool GetConsoleMode(IntPtr handle,out uint mode);
    [DllImport("kernel32.dll")] public static extern bool SetConsoleMode(IntPtr handle,uint mode);
    [DllImport("kernel32.dll")] public static extern bool GetConsoleScreenBufferInfo(IntPtr handle,out Info info);
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode)] public static extern bool ReadConsoleOutputCharacterW(IntPtr handle,StringBuilder text,uint length,Coord point,out uint read);
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode)] public static extern bool WriteConsoleInputW(IntPtr handle,Key[] keys,uint length,out uint written);
    [DllImport("kernel32.dll")] static extern bool SetConsoleWindowInfo(IntPtr handle,bool absolute,ref Rect rect);
    [DllImport("kernel32.dll")] static extern bool SetConsoleScreenBufferSize(IntPtr handle,Coord size);
    public static void Resize(short width,short height) {
        var current=CreateFile("CONOUT$",0xc0000000,3,IntPtr.Zero,3,0,IntPtr.Zero);
        var rect=new Rect { Left=0,Top=0,Right=(short)(width-1),Bottom=(short)(height-1) };
        try {
            if(!SetConsoleWindowInfo(current,true,ref rect))throw new Exception("Active viewport resize failed");
            SetConsoleScreenBufferSize(current,new Coord(width,height));
        }
        finally { CloseHandle(current); }
    }
    [StructLayout(LayoutKind.Explicit,Size=4,CharSet=CharSet.Unicode)] public struct Cell
    {
        [FieldOffset(0)] public char Character;
        [FieldOffset(2)] public ushort Attribute;
    }
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode)] public static extern bool ReadConsoleOutputW(IntPtr handle,[Out] Cell[] cells,Coord size,Coord origin,ref Rect region);
    [DllImport("kernel32.dll")] public static extern IntPtr GetConsoleWindow();
    [DllImport("kernel32.dll")] public static extern bool AttachConsole(uint pid);
    [DllImport("kernel32.dll")] public static extern bool FreeConsole();
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr window,out WindowRect rect);
    public static Info State()
    {
        Info info;
        var current=ActiveOutput();
        try { if(!GetConsoleScreenBufferInfo(current,out info))throw new Exception("No console buffer"); } finally { CloseHandle(current); }
        return info;
    }
    public static string Read(Rect region)
    {
        short width=(short)(region.Right-region.Left+1),height=(short)(region.Bottom-region.Top+1);
        var cells=new Cell[width*height];
        var current=ActiveOutput();
        try { if(!ReadConsoleOutputW(current,cells,new Coord(width,height),new Coord(0,0),ref region))throw new Exception("Console readback failed"); } finally { CloseHandle(current); }
        var text=new StringBuilder();
        for(int y=0;y<height;y++)
        {
            var row=new StringBuilder();
            for(int x=0;x<width;x++)
            {
                char c=cells[y*width+x].Character;
                row.Append(c<32?' ':c);
            }
            text.AppendLine(row.ToString().TrimEnd());
        }
        return text.ToString();
    }
    public static string Line()
    {
        var info=State();
        return Read(new Rect
        {
            Left=0,Right=(short)(info.Size.X-1),Top=info.Cursor.Y,Bottom=info.Cursor.Y
        }
        ).TrimEnd();
    }
    public static string Screen()
    {
        return Read(State().Window);
    }
    public static void Send(string text)
    {
        foreach(char c in text)
        {
            var key=new Key
            {
                Type=1,Down=1,Repeat=1,VirtualKey=(short)(c=='\r'?13:c=='-'?0xBD:char.ToUpperInvariant(c)),Character=c
            }
            ;
            uint count;
            if(!WriteConsoleInputW(GetStdHandle(-10),new[]
            {
                key
            }
            ,1,out count)||count!=1)throw new Exception("Console key delivery failed");
        }
    }
}
public static class BreadPreviewProgram
{
    static string classes,root,evidence;
    static System.Diagnostics.Process app;
    static System.Diagnostics.Process recording;
    static bool automated,screenAutomated,homeOnly;
    static System.Collections.Generic.List<string> checks=new System.Collections.Generic.List<string>();
    static readonly StringBuilder observedPages=new StringBuilder();
    static int pageCapture;
    static readonly System.Text.Encoding Utf8=new System.Text.UTF8Encoding(false);
    static bool Record { get { return Environment.GetEnvironmentVariable("BREAD_RECORD")=="1"; } }
    static void BeginRecording() {
        if(!Record)return;
        var info=new System.Diagnostics.ProcessStartInfo(Environment.GetEnvironmentVariable("BREAD_FFMPEG"));
        info.Arguments="-hide_banner -loglevel warning -f gdigrab -framerate 24 -i \"title=Breadcrumbs - screen acceptance\" -an -c:v libx264 -preset ultrafast -crf 24 -pix_fmt yuv420p -vf \"pad=ceil(iw/2)*2:ceil(ih/2)*2\" \""+System.IO.Path.Combine(evidence,"walkthrough.mp4")+"\"";
        info.UseShellExecute=false; info.CreateNoWindow=true; info.RedirectStandardInput=true;
        recording=System.Diagnostics.Process.Start(info);
        System.Threading.Thread.Sleep(700);
        if(recording.HasExited)throw new Exception("Native window recording did not start");
    }
    static void Check(bool value,string message)
    {
        if(!value)throw new Exception(message);
        checks.Add("PASS "+message);
    }
    static void Wait(string prompt)
    {
        var clock=System.Diagnostics.Stopwatch.StartNew();
        while(clock.ElapsedMilliseconds<20000)
        {
            if(app.HasExited)throw new Exception("App exited before "+prompt);
            if(BreadConsole.Line().Contains(prompt)) {
                System.Threading.Thread.Sleep(80);
                if(BreadConsole.Line().Contains(prompt))return;
            }
            string line=BreadConsole.Line();
            if(line.Contains("Enter:next")) {
                observedPages.AppendLine(BreadConsole.Screen());
                if(screenAutomated && pageCapture++==0) Capture("screen-00-welcome");
                else if(Record) System.Threading.Thread.Sleep(2000); // Let recorded report pages be read.
                BreadConsole.Send("\r");
                var consumed=System.Diagnostics.Stopwatch.StartNew();
                while(BreadConsole.Line()==line && consumed.ElapsedMilliseconds<5000) System.Threading.Thread.Sleep(20);
                if(BreadConsole.Line()==line) throw new Exception("Report page input was not consumed");
            }
            System.Threading.Thread.Sleep(40);
        }
        throw new Exception("Prompt timeout "+prompt+" / "+BreadConsole.Line());
    }
    static void Answer(string prompt,string value)
    {
        Wait(prompt);
        short row = BreadConsole.State().Cursor.Y;
        BreadConsole.Send(value+"\r");
        if(screenAutomated) { System.Threading.Thread.Sleep(Record?650:200); return; }
        var clock = System.Diagnostics.Stopwatch.StartNew();
        while(BreadConsole.State().Cursor.Y == row)
        {
            if(app.HasExited) return;
            if(clock.ElapsedMilliseconds > 5000) throw new Exception("Input was not consumed at " + prompt);
            System.Threading.Thread.Sleep(20);
        }
    }
    static void Start(string extra)
    {
        var info=new System.Diagnostics.ProcessStartInfo();
        info.FileName=System.IO.Path.Combine(Environment.GetEnvironmentVariable("BREAD_JDK"),"java.exe");
        info.Arguments="-cp \""+classes+"\" breadcrumbs.Main "+extra;
        info.WorkingDirectory=root;
        info.UseShellExecute=false;
        app=System.Diagnostics.Process.Start(info);
    }
    static void Capture(string name)
    {
        if(Record)System.Threading.Thread.Sleep(1600);
        System.IO.File.WriteAllText(System.IO.Path.Combine(evidence,name+".txt"),BreadConsole.Screen(),Utf8);
        var window = BreadConsole.GetConsoleWindow();
        var rect=new BreadConsole.WindowRect();
        if((!automated || screenAutomated) && BreadConsole.GetWindowRect(window,out rect)&&rect.Right>rect.Left&&rect.Bottom>rect.Top)
        {
            using(var bitmap=new System.Drawing.Bitmap(rect.Right-rect.Left,rect.Bottom-rect.Top))
            {
                using(var graphics=System.Drawing.Graphics.FromImage(bitmap))
                {
                    graphics.CopyFromScreen(rect.Left,rect.Top,0,0,bitmap.Size);
                }
                bitmap.Save(System.IO.Path.Combine(evidence,name+".png"),System.Drawing.Imaging.ImageFormat.Png);
            }
        }
    }
    static void Changed(string before)
    {
        var clock=System.Diagnostics.Stopwatch.StartNew();
        while(clock.ElapsedMilliseconds<9000)
        {
            if(BreadConsole.Screen()!=before)return;
            System.Threading.Thread.Sleep(40);
        }
        throw new Exception("No live frame change observed");
    }
    static void Arrow(short code) {
        uint written; var key=new BreadConsole.Key { Type=1,Down=1,Repeat=1,VirtualKey=code,Character=(char)0 };
        if(!BreadConsole.WriteConsoleInputW(BreadConsole.GetStdHandle(-10),new[]{key},1,out written))throw new Exception("Arrow key delivery failed");
    }
    static int ScreenCheck() {
        Console.Title="Breadcrumbs - screen acceptance";
        BeginRecording();
        string storage=System.IO.Path.Combine(root,"screen-native-storage");
        Start("--ui=screen --terminal-diagnostics --storage-root=\""+storage+"\"");
        Wait("Breadcrumbs >"); Capture("screen-01-home");
        Check(BreadConsole.Screen().Contains("BREADCRUMBS /"),"Enhanced viewport is active");
        Check(BreadConsole.Screen().StartsWith("/"),"Home retains its top border without terminal scrolling");
        var viewport=BreadConsole.State().Window;
        int columns=viewport.Right-viewport.Left+1,rows=viewport.Bottom-viewport.Top+1;
        if(columns>=160 && rows>=52) Check(BreadConsole.Screen().Contains("+##%%%%##+"),"Original polished B3 lettering is visible within Home");
        else {
            Check(BreadConsole.Screen().Contains("BREADCRUMBS / Home"),"Compact Home retains approved text branding at the measured viewport");
            checks.Add("NOT_PROVEN full B3 art in this run: measured "+columns+"x"+rows+"; full artwork requires160x52");
        }
        Check(BreadConsole.Screen().Contains("Leave context behind. Pick up where work stopped."),"Brand tagline shares the Home screen");
        for(int key=0;key<=8;key++) Check(BreadConsole.Screen().Contains("["+key+"]"),"Home action "+key+" is visible");
        string before=BreadConsole.Screen(); Changed(before);
        if(Record) System.Threading.Thread.Sleep(17500); // Reading/idle demonstration, never a production delay.
        Capture("screen-02-idle");
        BreadConsole.Send("2"); Wait("Breadcrumbs > 2"); before=BreadConsole.Screen(); Changed(before);
        Check(BreadConsole.Line().Contains("Breadcrumbs > 2"),"Partial numeric choice survives live motion");
        if(homeOnly) {
            BreadConsole.Send("\x15"); Answer("Breadcrumbs >","0");
            Check(app.WaitForExit(8000)&&app.ExitCode==0,"Unified Home exits and restores the terminal");
            Capture("screen-03-home-restored-shell"); return 0;
        }
        BreadConsole.Send("\r"); Answer("Entry mode:","1"); Wait("Project display name:");
        Check(!BreadConsole.Screen().Contains("Open saved project"),"Home is replaced before setup");
        BreadConsole.Send("Campus food drive"); Wait("Project display name: Campus food drive"); before=BreadConsole.Screen();
        System.Threading.Thread.Sleep(900); Check(before==BreadConsole.Screen(),"Free text has no background animation"); BreadConsole.Send("\r");
        Answer("Project type","1"); Answer("Project goal","Coordinate a campus food drive for 120 students");
        Answer("Planned deliverable","Volunteer roster and collection schedule"); Answer("Next action","Confirm collection points with the student council");
        Answer("Responsible person",""); Answer("Blockers or pending",""); Answer("Optional existing EMPTY","");
        Wait("Overview action:"); Capture("screen-03-overview"); Answer("Overview action:","4");
        Answer("Confirm this starting record","yes"); Answer("Save this project","yes"); Wait("Breadcrumbs >");
        for(int i=0;i<10 && !BreadConsole.Line().Contains("Breadcrumbs > 7");i++) { Arrow(40); System.Threading.Thread.Sleep(80); }
        Wait("Breadcrumbs > 7"); BreadConsole.Send("\r"); Wait("Workspace >"); Capture("screen-04-arrow-workspace");
        Check(BreadConsole.Screen().Contains("Project workspace"),"Arrow selection enters the original workspace route");
        Answer("Workspace >","13"); Wait("Map >"); Capture("screen-05-map");
        Check(!BreadConsole.Screen().Contains("Record progress update"),"Workspace is replaced by the map");
        Answer("Map >","1"); Answer("Section >","1"); Wait("Record >"); Capture("screen-06-source-detail");
        Answer("Record >","0"); Answer("Section >","0"); Answer("Map >","0");
        Answer("Workspace >","2"); Wait("Progress summary:"); Capture("screen-06a-progress-form"); Answer("Progress summary:","Confirmed three collection points and drafted the volunteer roster");
        Answer("Blockers or pending","none"); Answer("Next action:","Confirm volunteer shifts before Friday");
        Answer("Update action:","yes"); Wait("Workspace >"); Capture("screen-07-saved-update");
        Answer("Workspace >","12"); Wait("Workspace >");
        BreadConsole.Resize(80,24);
        var resized=System.Diagnostics.Stopwatch.StartNew();
        while(BreadConsole.State().Window.Right-BreadConsole.State().Window.Left+1!=80 && resized.ElapsedMilliseconds<5000) System.Threading.Thread.Sleep(20);
        bool narrow=BreadConsole.State().Window.Right-BreadConsole.State().Window.Left+1==80;
        if(narrow) Check(true,"Active alternate viewport resized to 80 columns");
        else checks.Add("NOT_PROVEN live host resize: requested 80 columns, host retained "+(BreadConsole.State().Window.Right-BreadConsole.State().Window.Left+1));
        System.Threading.Thread.Sleep(250); Answer("Workspace >","?"); Wait("Workspace >"); Capture("screen-08-narrow");
        if(narrow) Check(BreadConsole.Screen().Split('\n')[1].TrimEnd().Length==79,"Narrow viewport is measured and recomposed at 80 columns");
        Answer("Workspace >","0"); Answer("Breadcrumbs >","0"); Check(app.WaitForExit(8000)&&app.ExitCode==0,"Enhanced application exits cleanly"); Capture("screen-09-restored-shell");
        // Keep a recorded window's capture dimensions stable. The unrecorded
        // acceptance run still exercises a real80x24 returning process.
        if(Record) Console.SetWindowSize(Math.Min(160,Console.LargestWindowWidth),Math.Min(52,Console.LargestWindowHeight));
        else Console.SetWindowSize(80,24);
        Start("--ui=screen --no-animation --storage-root=\""+storage+"\"");
        Answer("Breadcrumbs >","1"); Answer("Project number","1"); Answer("Breadcrumbs >","7"); observedPages.Clear(); Answer("Workspace >","1"); Wait("Workspace >");
        observedPages.AppendLine(BreadConsole.Screen());
        Check(observedPages.ToString().Contains("Confirm volunteer shifts before Friday"),"Restart shows the actual saved next action across reachable summary pages"); Capture("screen-10-returning");
        System.IO.File.WriteAllText(System.IO.Path.Combine(evidence,"returning-summary-pages.txt"),observedPages.ToString(),Utf8);
        Answer("Workspace >","0"); Answer("Breadcrumbs >","0"); Check(app.WaitForExit(8000)&&app.ExitCode==0,"Reduced motion keeps all navigation and exits cleanly");
        checks.Add("NOT_PROVEN independent human keyboard input, Windows Terminal, VS Code and Linux JVM");
        return 0;
    }
    static int ClassroomCheck()
    {
        Console.Title="Breadcrumbs - The Bread classroom acceptance";
        Start("--classroom");
        ClassroomCapture("01-start", "Records: 5");
        BreadConsole.Send("9\r");
        ClassroomCapture("02-structure", "HEAD=DEMO-000001 | TAIL=DEMO-000005");
        Answer("Choose an option:", "1");
        Answer("Title (required", "Review room layout");
        Answer("Summary (blank", "Draft the layout for the demonstration.");
        Answer("Status [", "PLANNED");
        Answer("Confirm creation?", "yes");
        ClassroomCapture("03-tail-insert", "Success: DEMO-NEW-000001");
        Answer("Choose an option:", "4");
        Answer("Checkpoint ID:", "DEMO-000003");
        ClassroomCapture("04-found-record", "Found DEMO-000003");
        Answer("Choose an option:", "5");
        Answer("Checkpoint ID to delete:", "DEMO-MISSING");
        ClassroomCapture("05-missing-id", "Not found");
        Answer("Choose an option:", "3");
        Answer("Position (0-based", "999");
        ClassroomCapture("06-invalid-position", "Position must be between");
        BreadConsole.Send("0\r");
        Check(app.WaitForExit(8000) && app.ExitCode==0,"Classroom app exits cleanly");
        return 0;
    }
    static void ClassroomCapture(string name, string expected)
    {
        var clock=System.Diagnostics.Stopwatch.StartNew();
        while(clock.ElapsedMilliseconds<10000 && !app.HasExited)
        {
            string screen=BreadConsole.Screen();
            if(BreadConsole.Line().Contains("Choose an option:") && screen.Contains(expected)
                && screen.Contains("BREADCRUMBS | The Bread | DOUBLY LINKED LIST"))
            {
                System.Threading.Thread.Sleep(180); // Allow the on-screen console to paint the verified buffer.
                Capture(name);
                Check(true,name+" shows "+expected+" with the actual group title");
                return;
            }
            System.Threading.Thread.Sleep(40);
        }
        throw new Exception("Classroom viewport did not show "+name+" and "+expected+" together with group title.");
    }

    public static int Main(string[] args)
    {
        if(args.Length==2 && args[0]=="--close-preview")
        {
            uint pid;
            if(!uint.TryParse(args[1],out pid))return 2;
            BreadConsole.FreeConsole();
            if(!BreadConsole.AttachConsole(pid))return 3;
            try
            {
                if(!BreadConsole.Line().StartsWith("Preview >"))return 4;
                BreadConsole.Send("\r");
                return 0;
            }
            finally
            {
                BreadConsole.FreeConsole();
            }
        }
        if(args.Length<3)return 2;
        classes=args[0];
        root=args[1];
        evidence=args[2];
        System.IO.Directory.CreateDirectory(evidence);
        BreadConsole.BindConsoleInput();
        uint oldMode;
        var handle=BreadConsole.GetStdHandle(-11);
        if(!BreadConsole.GetConsoleMode(handle,out oldMode)||!BreadConsole.SetConsoleMode(handle,oldMode|4))return 3;
        Console.OutputEncoding=Utf8;
        Environment.SetEnvironmentVariable("ANSICON","1");
        Environment.SetEnvironmentVariable("NO_COLOR",null);
        Environment.SetEnvironmentVariable("TERM",null);
        Console.Title="Bread Support - reference symbol loaf";
        try
        {
            try
            {
                Console.SetBufferSize(Math.Max(160,Console.BufferWidth),Math.Max(3000,Console.BufferHeight));
                Console.SetWindowSize(Math.Min(160,Console.LargestWindowWidth),Math.Min(52,Console.LargestWindowHeight));
            }
            catch
            {
            }
            homeOnly=args.Length>3&&args[3]=="--home-auto";
            screenAutomated=args.Length>3&&(args[3]=="--screen-auto"||homeOnly);
            automated=args.Length>3&&(args[3]=="--auto"||screenAutomated);
            if(args.Length>3 && args[3]=="--classroom-auto") return ClassroomCheck();
            if(screenAutomated) return ScreenCheck();
            if(!automated)
            {
                bool integrated = args.Length > 3 && args[3] == "--app";
                if(integrated) Console.Title = "Breadcrumbs - framed menu and companion";
                Start(integrated ? "--storage-root=\"" + System.IO.Path.Combine(root,"interactive-demo-storage") + "\"" : "--bread-preview=idle");
                Wait(integrated ? "Breadcrumbs >" : "Preview >");
                Capture("symbol-preview");
                app.WaitForExit();
                return app.ExitCode;
            }
            Console.Title="Bread Support - CMD acceptance";
            string storage=System.IO.Path.Combine(root,"cmd-native-storage");
            Start("--classic-ui --storage-root=\""+storage+"\"");
            Wait("Breadcrumbs >");
            Capture("01-full-main-menu");
            string before=BreadConsole.Screen();
            short row=BreadConsole.State().Cursor.Y;
            Changed(before);
            Capture("02-idle-frame");
            Check(BreadConsole.State().Cursor.Y==row,"Idle frames do not create scrollback rows");
            BreadConsole.Send("2");
            Wait("Breadcrumbs > 2");
            Capture("03-partial-number");
            before=BreadConsole.Screen();
            Changed(before);
            Check(BreadConsole.Line().Contains("Breadcrumbs > 2"),"Partially typed number survives animation");
            BreadConsole.Send("\r");
            Answer("Entry mode:","1");
            Wait("Project display name:");
            BreadConsole.Send("Symbol bread demo");
            Wait("Project display name: Symbol bread demo");
            before=BreadConsole.Screen();
            System.Threading.Thread.Sleep(2000);
            Check(BreadConsole.Screen()==before,"Long input remains static while typing pauses");
            Capture("04-long-input");
            BreadConsole.Send("\r");
            Answer("Project type","1");
            Answer("Project goal","Inspect the symbol mascot");
            Answer("Planned deliverable","A synthetic acceptance record");
            Answer("Next action","Review the console proof");
            Answer("Responsible person","Demo owner");
            Answer("Blockers or pending items","none");
            Answer("Optional existing EMPTY directory","");
            Answer("Overview action:","4");
            Answer("Confirm this starting record","yes");
            Answer("Save this project on this computer?","yes");
            Answer("Breadcrumbs >","7");
            Wait("Workspace >");
            Capture("05-compact-workspace");
            Answer("Workspace >","13");
            Wait("Map >");
            Capture("06-project-map");
            Answer("Map >","0");
            Answer("Workspace >","2");
            Answer("Progress summary:","Recorded a synthetic console check");
            Answer("Blockers or pending items","none");
            Answer("Next action:","Read the evidence");
            Wait("Update action:");
            Capture("07-update-confirmation");
            Answer("Update action:","yes");
            Wait("Workspace >");
            Capture("08-saved-update");
            Answer("Workspace >","12");
            Wait("Workspace >");
            string[] saved=System.IO.Directory.GetDirectories(System.IO.Path.Combine(storage,"workspaces"));
            Check(saved.Length==1&&System.IO.File.Exists(System.IO.Path.Combine(saved[0],"state.txt")),"One saved workspace with retained recovery generations");
            Console.SetWindowSize(80,24);
            System.Threading.Thread.Sleep(600);
            Capture("09-resize-static-input");
            Answer("Workspace >","?");
            Wait("Workspace >");
            Capture("10-narrower-workspace");
            Answer("Workspace >","0");
            Answer("Breadcrumbs >","0");
            Check(app.WaitForExit(5000)&&app.ExitCode==0,"First application exits cleanly");
            Capture("11-clean-exit");
            Console.SetWindowSize(Math.Min(160,Console.LargestWindowWidth),Math.Min(52,Console.LargestWindowHeight));
            Start("--classic-ui --storage-root=\""+storage+"\"");
            Answer("Breadcrumbs >","1");
            Answer("Project number","1");
            Answer("Breadcrumbs >","7");
            Answer("Workspace >","1");
            Wait("Workspace >");
            Check(BreadConsole.Screen().Contains("Read the evidence"),"Returning process shows the actual recorded next step");
            Capture("12-returning-user");
            Answer("Workspace >","0");
            Answer("Breadcrumbs >","0");
            Check(app.WaitForExit(5000)&&app.ExitCode==0,"Returning application exits cleanly");
            checks.Add("NOT_PROVEN human typing and other terminal hosts");
            return 0;
        }
        catch(Exception failure)
        {
            try
            {
                Capture("failure");
            }
            catch
            {
            }
            System.IO.File.WriteAllText(System.IO.Path.Combine(evidence,"error.txt"),failure.ToString(),Utf8);
            return 1;
        }
        finally
        {
            if(app!=null&&!app.HasExited)
            {
                app.Kill();
                app.WaitForExit();
            }
            BreadConsole.SetConsoleMode(handle,oldMode);
            if(recording!=null&&!recording.HasExited) {
                recording.StandardInput.WriteLine("q"); recording.StandardInput.Flush();
                if(!recording.WaitForExit(10000))recording.Kill();
            }
            System.IO.File.WriteAllText(System.IO.Path.Combine(evidence,"observed-pages.txt"),observedPages.ToString(),Utf8);
            System.IO.File.WriteAllLines(System.IO.Path.Combine(evidence,"checks.txt"),checks,Utf8);
        }
    }
}
