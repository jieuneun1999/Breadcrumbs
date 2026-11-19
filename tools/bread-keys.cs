using System;
using System.Runtime.InteropServices;
public static class BreadKeys
{
    [StructLayout(LayoutKind.Sequential)]public struct Point
    {
        public short X,Y;
    }
    [StructLayout(LayoutKind.Sequential)]public struct Rect
    {
        public short Left,Top,Right,Bottom;
    }
    [StructLayout(LayoutKind.Sequential)]public struct Info
    {
        public Point Size,Cursor;
        public short Attribute;
        public Rect Window;
        public Point Maximum;
    }
    [StructLayout(LayoutKind.Explicit,Size=20,CharSet=CharSet.Unicode)]public struct Key
    {
        [FieldOffset(0)]public short Type;
        [FieldOffset(4)]public int Down;
        [FieldOffset(8)]public short Repeat;
        [FieldOffset(10)]public short Virtual;
        [FieldOffset(14)]public char Character;
    }
    [DllImport("kernel32.dll")]static extern bool AttachConsole(uint id);
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode)]static extern IntPtr CreateFile(string name,uint access,uint share,IntPtr security,uint create,uint flags,IntPtr template);
    [DllImport("kernel32.dll")]static extern bool GetConsoleScreenBufferInfo(IntPtr handle,out Info info);
    [DllImport("kernel32.dll")]static extern bool GetConsoleMode(IntPtr handle,out uint mode);
    [DllImport("kernel32.dll")]static extern IntPtr GetStdHandle(int standardHandle);
    [DllImport("kernel32.dll")]static extern bool SetConsoleMode(IntPtr handle,uint mode);
    [DllImport("kernel32.dll")]static extern bool GetNumberOfConsoleInputEvents(IntPtr handle,out uint count);
    [DllImport("kernel32.dll",CharSet=CharSet.Unicode)]static extern bool ReadConsoleInputW(IntPtr handle,out Key key,uint count,out uint read);
    [DllImport("kernel32.dll")]static extern IntPtr GetConsoleWindow();
    [DllImport("user32.dll")]static extern bool IsWindowVisible(IntPtr window);
    public static string Palette()
    {
        return IsWindowVisible(GetConsoleWindow()) ? "ANSI" : "RGB";
    }
    [DllImport("kernel32.dll")]static extern bool CloseHandle(IntPtr handle);
    static IntPtr input,output;
    public static void Open(uint parent)
    {
        AttachConsole(parent);
        input=CreateFile("CONIN$",0x80000000,3,IntPtr.Zero,3,0,IntPtr.Zero);
        output=CreateFile("CONOUT$",0x80000000,3,IntPtr.Zero,3,0,IntPtr.Zero);
        if(input==new IntPtr(-1)||output==new IntPtr(-1))throw new Exception("No native console");
        uint mode;
        if(!GetConsoleMode(output,out mode)||(mode&4)==0)throw new Exception("VT cursor control unavailable");
    }
    public static string Size()
    {
        Info info;
        var current=CreateFile("CONOUT$",0x80000000,3,IntPtr.Zero,3,0,IntPtr.Zero);
        try { if(!GetConsoleScreenBufferInfo(current,out info))throw new Exception("No console size"); }
        finally { CloseHandle(current); }
        return (info.Window.Right-info.Window.Left+1)+" "+(info.Window.Bottom-info.Window.Top+1);
    }
    public static bool Pending()
    {
        uint count;
        if(!GetNumberOfConsoleInputEvents(input,out count))throw new Exception("Console input unavailable");
        return count>0;
    }
    public static string Read()
    {
        Key key;
        uint read;
        if(!ReadConsoleInputW(input,out key,1,out read))throw new Exception("Console input failed");
        if(key.Type!=1||key.Down==0||key.Virtual==46)return "";
        if(key.Virtual>=37&&key.Virtual<=40)
        {
            int code=key.Virtual==38?0xe000:key.Virtual==40?0xe001:key.Virtual==37?0xe002:0xe003;
            return new string((char)code,Math.Min(128,Math.Max(1,(int)key.Repeat)));
        }
        if(key.Character==0)return "";
        return new string(key.Character,Math.Min(128,Math.Max(1,(int)key.Repeat)));
    }
    internal static int Launch(string[] args)
    {
        const uint ProcessedOutput = 0x0001;
        const uint VirtualTerminal = 0x0004;
        var outputHandle = GetStdHandle(-11);
        uint originalMode = 0, inputMode;
        bool virtualTerminal = GetConsoleMode(GetStdHandle(-10), out inputMode)
            && GetConsoleMode(outputHandle, out originalMode)
            && SetConsoleMode(outputHandle, originalMode | ProcessedOutput | VirtualTerminal);
        if (!virtualTerminal)
        {
            Console.Error.WriteLine("The colored Breadcrumbs interface needs an interactive Windows console. Open run.bat directly or run it from Command Prompt.");
            return 1;
        }
        try
        {
            if (IsWindowVisible(GetConsoleWindow())) FitDemoWindow();
            var java = new System.Diagnostics.ProcessStartInfo("java.exe");
            java.UseShellExecute = false;
            string classes = System.IO.Path.GetDirectoryName(typeof(BreadKeys).Assembly.Location);
            java.Arguments = "-Dbreadcrumbs.fullUi=true -cp " + QuoteArgument(classes) + " breadcrumbs.Main";
            for (int index = 1; index < args.Length; index++)
            {
                java.Arguments += " " + QuoteArgument(args[index]);
            }
            java.EnvironmentVariables["ANSICON"] = "1";
            java.EnvironmentVariables.Remove("NO_COLOR");
            if (String.Equals(java.EnvironmentVariables["TERM"], "dumb", StringComparison.OrdinalIgnoreCase))
                java.EnvironmentVariables.Remove("TERM");
            using (var child = System.Diagnostics.Process.Start(java))
            {
                child.WaitForExit();
                return child.ExitCode;
            }
        }
        catch (System.ComponentModel.Win32Exception error)
        {
            Console.Error.WriteLine("Breadcrumbs could not start Java. " + error.Message);
            return 1;
        }
        finally
        {
            SetConsoleMode(outputHandle, originalMode);
        }
    }
    static void FitDemoWindow()
    {
        // Keep this in step with TerminalScreenManager's full Home viewport.
        const int HomeColumns = 160, HomeRows = 52;
        try
        {
            int width = Math.Max(HomeColumns, Console.WindowWidth);
            int height = Math.Max(HomeRows, Console.WindowHeight);
            if (width == Console.WindowWidth && height == Console.WindowHeight) return;
            if (width > Console.LargestWindowWidth || height > Console.LargestWindowHeight) return;
            Console.SetBufferSize(Math.Max(width, Console.BufferWidth), Math.Max(height, Console.BufferHeight));
            Console.SetWindowSize(width, height);
        }
        catch (System.IO.IOException)
        {
            // Some Windows hosts own their viewport. The app composes for the measured size.
        }
        catch (ArgumentOutOfRangeException)
        {
            // The console may have moved to a smaller display during startup.
        }
    }
    static string QuoteArgument(string value)
    {
        string escaped = System.Text.RegularExpressions.Regex.Replace(value, @"(\\*)(""|$)",
            match => match.Groups[1].Value + match.Groups[1].Value
                + (match.Groups[2].Value.Length == 0 ? "" : "\\\""));
        return "\"" + escaped + "\"";
    }
    public static void Close()
    {
        CloseHandle(input);
        CloseHandle(output);
    }
}
public static class BreadKeyProgram
{
    public static int Main(string[] args)
    {
        if (args.Length > 0 && args[0] == "--launch") return BreadKeys.Launch(args);
        uint parent;
        if(args.Length!=1 || !uint.TryParse(args[0],out parent))return 2;
        using(var events=new System.IO.StreamWriter(System.Console.OpenStandardOutput(),new System.Text.UTF8Encoding(false)))
        {
            events.AutoFlush=true;
            try
            {
                BreadKeys.Open(parent);
                events.WriteLine("READY "+BreadKeys.Size()+" "+BreadKeys.Palette());
                var clock=System.Diagnostics.Stopwatch.StartNew();
                var alive=System.Diagnostics.Stopwatch.StartNew();
                while(true)
                {
                    if(alive.ElapsedMilliseconds>=1000)
                    {
                        using(var process=System.Diagnostics.Process.GetProcessById((int)parent))
                        {
                            if(process.HasExited)return 0;
                        }
                        alive.Restart();
                    }
                    if(clock.ElapsedMilliseconds>=125)
                    {
                        events.WriteLine("T "+BreadKeys.Size());
                        clock.Restart();
                    }
                    for(int i=0;i<128 && BreadKeys.Pending();i++)
                    {
                        foreach(char key in BreadKeys.Read())events.WriteLine("K "+(int)key);
                    }
                    System.Threading.Thread.Sleep(20);
                }
            }
            catch
            {
                return 2;
            }
            finally
            {
                BreadKeys.Close();
            }
        }
    }
}
