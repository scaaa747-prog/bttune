using System;
using System.IO;
using System.Windows.Forms;
using Microsoft.Web.WebView2.WinForms;
using Microsoft.Web.WebView2.Core;

namespace BTTUNE
{
    static class Program
    {
        [STAThread]
        static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }
    }

    public class MainForm : Form
    {
        private WebView2 webView;

        public MainForm()
        {
            this.Text = "BTTUNE";
            this.Width = 1280;
            this.Height = 820;
            this.StartPosition = FormStartPosition.CenterScreen;
            this.BackColor = System.Drawing.Color.FromArgb(10, 10, 14);

            webView = new WebView2
            {
                Dock = DockStyle.Fill
            };
            this.Controls.Add(webView);
            InitializeAsync();
        }

        private async void InitializeAsync()
        {
            string userDataFolder = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "BTTUNE_Cache");
            var env = await CoreWebView2Environment.CreateAsync(null, userDataFolder);
            await webView.EnsureCoreWebView2Async(env);

            string htmlPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "index.html");
            if (File.Exists(htmlPath))
            {
                webView.CoreWebView2.Navigate("file:///" + htmlPath.Replace("\\", "/"));
            }
            else
            {
                webView.CoreWebView2.NavigateToString(GetFallbackHtml());
            }
        }

        private string GetFallbackHtml()
        {
            return "<html><body style='background:#0A0A0E;color:white;display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;'><h1>BTTUNE Loading...</h1></body></html>";
        }
    }
}
