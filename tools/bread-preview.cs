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
    [DllImport("kernel32.dll")] public static extern bool GetConsoleMode(IntPtr handle,out uint mode);
    [DllImport("kernel32.dll")] public static extern bool SetConsoleMode(IntPtr handle,uint mode);
    [DllImport("kernel32.dll")] public static extern bool GetConsoleScreenBufferInfo(IntPtr handle,out Info info);
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode)] public static extern bool ReadConsoleOutputCharacterW(IntPtr handle,StringBuilder text,uint length,Coord point,out uint read);
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode)] public static extern bool WriteConsoleInputW(IntPtr handle,Key[] keys,uint length,out uint written);
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
        if(!GetConsoleScreenBufferInfo(GetStdHandle(-11),out info))throw new Exception("No console buffer");
        return info;
    }
    public static string Read(Rect region)
    {
        short width=(short)(region.Right-region.Left+1),height=(short)(region.Bottom-region.Top+1);
        var cells=new Cell[width*height];
        if(!ReadConsoleOutputW(GetStdHandle(-11),cells,new Coord(width,height),new Coord(0,0),ref region))throw new Exception("Console readback failed");
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
                Type=1,Down=1,Repeat=1,VirtualKey=(short)(c=='\r'?13:char.ToUpperInvariant(c)),Character=c
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
    static bool automated;
    static System.Collections.Generic.List<string> checks=new System.Collections.Generic.List<string>();
    static readonly System.Text.Encoding Utf8=new System.Text.UTF8Encoding(false);
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
            if(BreadConsole.Line().Contains(prompt))return;
            System.Threading.Thread.Sleep(40);
        }
        throw new Exception("Prompt timeout "+prompt+" / "+BreadConsole.Line());
    }
    static void Answer(string prompt,string value)
    {
        Wait(prompt);
        short row = BreadConsole.State().Cursor.Y;
        BreadConsole.Send(value+"\r");
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
        System.IO.File.WriteAllText(System.IO.Path.Combine(evidence,name+".txt"),BreadConsole.Screen(),Utf8);
        var window = BreadConsole.GetConsoleWindow();
        var rect=new BreadConsole.WindowRect();
        if(!automated && BreadConsole.GetWindowRect(window,out rect)&&rect.Right>rect.Left&&rect.Bottom>rect.Top)
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
        uint oldMode;
        var handle=BreadConsole.GetStdHandle(-11);
        if(!BreadConsole.GetConsoleMode(handle,out oldMode)||!BreadConsole.SetConsoleMode(handle,oldMode|4))return 3;
        Console.OutputEncoding=Utf8;
        Environment.SetEnvironmentVariable("ANSICON","1");
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
            automated=args.Length>3&&args[3]=="--auto";
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
            Start("--storage-root=\""+storage+"\"");
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
            Start("--storage-root=\""+storage+"\"");
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
            System.IO.File.WriteAllLines(System.IO.Path.Combine(evidence,"checks.txt"),checks,Utf8);
        }
    }
}
