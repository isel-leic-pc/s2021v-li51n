using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Examples.Asynch;
using Xunit;
using Xunit.Abstractions;

namespace Tests.Asynch
{
    public class AsyncUnarySemaphoreTests
    {
        private async Task UseSemaphore(int id, AsyncUnarySemaphore semaphore)
        {
            for (var i = 0; i < 4; ++i)
            {
                await semaphore.AcquireAsync();
                Log($"Unit acquired by {id}");
                // use the semaphore
                await Task.Delay(500);
                Log($"Unit will be released by {id}");
                semaphore.Release();
                Log($"Unit was released by {id}");
            }   
        }

        // [13|non-pool|09:22:33.496]Unit will be released by 0 <-
        // [13|non-pool|09:22:33.496]Unit acquired by 1
        // [13|non-pool|09:22:33.497]Unit was released by 0 <-
        [Fact]
        public Task First()
        {
            Log("starting test");
            var sem = new AsyncUnarySemaphore(1);
            var tasks = Enumerable.Range(0, 4)
                .Select(ix => UseSemaphore(ix, sem))
                .ToList();
            return Task.WhenAll(tasks);
            
            // UseSemaphore(0, sem);
            // UseSemaphore(1, sem);
            // ...
        }
        
        
        private readonly ITestOutputHelper _output;

        public AsyncUnarySemaphoreTests(ITestOutputHelper output)
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