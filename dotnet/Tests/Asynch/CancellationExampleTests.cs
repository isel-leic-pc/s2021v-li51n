using System;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;

namespace Tests.Asynch
{
    public class CancellationExampleTests
    {
        [Fact]
        public void First()
        {
            var tcs = new CancellationTokenSource();
            var ct = tcs.Token;
            Log($"IsCancellationRequested: {ct.IsCancellationRequested}");
            Log("Calling tcs.Cancel()");
            tcs.Cancel();
            Log($"IsCancellationRequested: {ct.IsCancellationRequested}");
        }
        
        [Fact]
        public async Task Second()
        {
            var tcs = new CancellationTokenSource();
            var ct = tcs.Token;
            var httpClient = new HttpClient();
            var t1 = httpClient.GetAsync("https://httpbin.org/delay/4", ct);
            Log("GET started");
            await Task.Delay(1000);
            Log("Requesting cancellation");
            tcs.Cancel();
            try
            {
                await t1;
            }
            catch (TaskCanceledException e)
            {
                Log("TaskCanceledException was catched");
            }
        }

        [Fact]
        public void Third()
        {
            var sem = new SemaphoreSlim(0);
            var cts = new CancellationTokenSource();
            var ct = cts.Token;
            ct.Register(() =>
            {
                Log("Cancellation callback was called");
                sem.Release(1);
            });
            var timer = new Timer(ignored =>
            {
                Log("Request cancellation");
                cts.Cancel();
                Log("Cancellation requested");
            }, null, 1000, Timeout.Infinite);

            Log("Before sem.Wait()");
            sem.Wait();
            Log("Test ending");
        }
        
        [Fact]
        public void Fourth()
        {
            var sem = new SemaphoreSlim(0);
            var cts = new CancellationTokenSource();
            var ct = cts.Token;
            ct.Register(() =>
            {
                Log("Cancellation callback was called");
                sem.Release(1);
            });
            cts.CancelAfter(1000);

            Log("Before sem.Wait()");
            sem.Wait();
            Log("Test ending");
        }
        

        private readonly ITestOutputHelper _output;

        public CancellationExampleTests(ITestOutputHelper output)
        {
            _output = output;
        }

        private void Log(string s)
        {
            _output.WriteLine("[{0,2}|{1,8}|{2:hh:mm:ss.fff}]{3}",
                Thread.CurrentThread.ManagedThreadId,
                Thread.CurrentThread.IsThreadPoolThread ? "pool" : "non-pool", DateTime.Now, s);
        }
    }
}