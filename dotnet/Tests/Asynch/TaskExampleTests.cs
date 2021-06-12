using System;
using System.Net;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;

namespace Tests.Asynch
{
    public class TaskExampleTests
    {
        [Fact]
        public void http_get_example_using_blocking()
        {
            var client = new HttpClient();
            var task = client.GetAsync("https://httpbin.org/delay/4");
            var response = task.Result;
            Log($"{response.StatusCode}");
            Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        }

        [Fact]
        public void http_get_example_using_continue_with()
        {
            var sem = new SemaphoreSlim(0);
            var client = new HttpClient();
            var task = client.GetAsync("https://httpbin.org/delay/4");
            Log($"{task.Status}");
            var response = task.ContinueWith(t =>
            {
                Log($"{task.Status}");
                var res = task.Result;
                Log($"Result is {res.StatusCode}");
                sem.Release();
            });
            sem.Wait();
        }

        [Fact]
        public void http_get_example_using_continue_with_errors()
        {
            var sem = new SemaphoreSlim(0);
            var client = new HttpClient();
            Task<HttpResponseMessage> task = client.GetAsync("https://httpbin2.org/delay/1");
            Log($"{task.Status}");
            task.ContinueWith(t =>
            {
                Log($"{task.Status}");
                Log($"{task.Exception.Message}");
                try
                {
                    Log($"{task.Result}");
                }
                catch (Exception e)
                {
                    Log($"Catched {e.Message}");
                }

                sem.Release();
            });
            sem.Wait();
        }

        [Fact]
        public void http_get_example_using_continue_with_2()
        {
            var sem = new SemaphoreSlim(0);
            var client = new HttpClient();
            var task = client.GetAsync("https://httpbin.org/delay/4");
            Log($"{task.Status}");
            Task<HttpStatusCode> task2 = task.ContinueWith(t =>
            {
                Log($"{t.Status}");
                var res = t.Result;
                Log($"Result is {res.StatusCode}");
                return res.StatusCode;
            });
            task2.ContinueWith(t =>
            {
                Log($"{t.Status}");
                var res = t.Result;
                Log($"Result is {res}");
                sem.Release();
            });
            sem.Wait();
        }

        [Fact]
        public void http_get_example_using_continue_with_3()
        {
            var sem = new SemaphoreSlim(0);
            var client = new HttpClient();
            var task = client.GetAsync("https://httpbin.org/delay/4");
            Log($"{task.Status}");
            Task task2 = task.ContinueWith(t =>
                {
                    Log($"{t.Status}");
                    var res = t.Result;
                    Log($"Result is {res.StatusCode}");
                    return res.StatusCode;
                })
                .ContinueWith(t =>
                {
                    Log($"{t.Status}");
                    var res = t.Result;
                    Log($"Result is {res}");
                    sem.Release();
                });
            sem.Wait();
        }

        [Fact]
        public void http_get_example_using_continue_with_4()
        {
            var sem = new SemaphoreSlim(0);
            var client = new HttpClient();
            var task = client.GetAsync("https://httpbin.org/delay/4");
            Log($"{task.Status}");
            Task<HttpResponseMessage> task2 = task.ContinueWith(t =>
            {
                Log($"{t.Status}");
                var res = t.Result;
                Log($"Result is {res.StatusCode}");
                return client.GetAsync("https://httpbin.org/delay/2");
            }).Unwrap();
            task2.ContinueWith(t =>
            {
                Log($"{t.Status}");
                var res = t.Result;
                Log($"Result is {res.StatusCode}");
                sem.Release();
            });

            sem.Wait();
        }

        [Fact]
        public Task http_get_example_using_continue_with_5()
        {
            var client = new HttpClient();
            var task = client.GetAsync("https://httpbin.org/delay/4");
            Log($"{task.Status}");
            Task<HttpResponseMessage> task2 = task.ContinueWith(t =>
            {
                Log($"{t.Status}");
                var res = t.Result;
                Log($"Result is {res.StatusCode}");
                return client.GetAsync("https://httpbin.org/delay/2");
            }).Unwrap();
            Task task3 = task2.ContinueWith(t =>
            {
                Log($"{t.Status}");
                var res = t.Result;
                Log($"Result is {res.StatusCode}");
            });

            Log("about to return");
            return task3;
        }

        [Fact]
        public Task WaitAll_example()
        {
            Log("started");
            var client = new HttpClient();
            var t1 = client.GetAsync("https://httpbin.org/delay/2");
            var t2 = client.GetAsync("https://httpbin.org/delay/4");
            var t3 = Task.WhenAll(t1, t2);
            return t3.ContinueWith(t =>
            {
                var result = t.Result;
                var first = result[0];
                var second = result[1];
                Log($"{t1.Result == first}, {t2.Result == second}");
            });
        }

        [Fact]
        public Task WaitAll_example_with_errors()
        {
            Log("started");
            var client = new HttpClient();
            var t1 = client.GetAsync("https://httpbin2.org/delay/2");
            var t2 = client.GetAsync("https://httpbin.org/delay/4");
            var t3 = Task.WhenAll(t1, t2);
            return t3.ContinueWith(t =>
            {
                Log("t3 completed");
                Log($"{t1.Status}, {t2.Status}");
                Log($"t3.Status = {t3.Status}");
                var ae = t.Exception;
                var first = ae.InnerExceptions[0];
                //var second = ae.InnerExceptions[1];
                //Log($"{first.Message}, {second.Message}");
                Log($"{first.Message}");
            });
        }

        [Fact]
        async Task asynchronous_methods()
        {
            async Task<string> AnAsynchronousFunction()
            {
                Log("async function started");
                var client = new HttpClient();
                var t1 = client.GetAsync("https://httpbin.org/delay/2");
                Log("after GetAsync");
                HttpResponseMessage response = await t1; // não é bloqueante!
                
                
                
                Log("after first await");
                var t2 = response.Content.ReadAsStringAsync();
                String body = await t2;
                Log("after second await");
                
                return body.ToUpper();
            }

            Log("before call");
            var t1 = await AnAsynchronousFunction();
            Log("after call");
            
        }
        
        [Fact]
        async Task<Task<string>> asynchronous_methods_with_exceptions()
        {
            async Task<string> AnAsynchronousFunction()
            {
               throw new Exception("Some Error");
            }

            Log("before call");
            var t1 = AnAsynchronousFunction();
            Log("after call");
            return t1;
        }

        [Fact]
        async Task TCS_example()
        {
            Log("before SleepAsync");
            await SleepAsync(2000);
            Log("after SleepAsync");
        }

        Task SleepAsync(int ms)
        {
            var tcs = new TaskCompletionSource<object>();
            void Callback(object ignore)
            {
                tcs.SetResult(null);
            }

            // TODO What happens if the Timer gets CGed?
            new Timer(Callback, null, ms, Timeout.Infinite);
            
            // return task
            return tcs.Task;
        }

        async Task<T> MyUnwrap1<T>(Task<Task<T>> wrappedTask)
        {
            return await await wrappedTask;
        }
        
        Task<T> MyUnwrap2<T>(Task<Task<T>> wrappedTask)
        {
            var tcs = new TaskCompletionSource<T>();

            wrappedTask.ContinueWith(t =>
            {
                tcs...
            });

            return tcs.Task;
        }

        private readonly ITestOutputHelper _output;

        public TaskExampleTests(ITestOutputHelper output)
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