using System.Collections.Generic;
using System.Threading.Tasks;

namespace Examples.Asynch
{
    public class AsyncUnarySemaphore
    {
        private class Request
        {
            public readonly TaskCompletionSource<object> Tcs = 
                new TaskCompletionSource<object>(TaskCreationOptions.RunContinuationsAsynchronously);
        }

        private readonly object _theLock = new object();
        private readonly LinkedList<Request> _queue = new LinkedList<Request>();
        private int _units;

        public AsyncUnarySemaphore(int initialUnits)
        {
            _units = initialUnits;
        }

        public Task AcquireAsync()
        {
            lock (_theLock)
            {
                // fast path
                if (_units > 0)
                {
                    _units -= 1;
                    return Task.CompletedTask;
                }

                var request = new Request();
                _queue.AddLast(request);

                return request.Tcs.Task;
            }
        }

        public void Release()
        {
            lock (_theLock)
            {
                var maybeFirst = _queue.First;
                if (maybeFirst != null)
                {
                    maybeFirst.Value.Tcs.SetResult(null);
                    _queue.RemoveFirst();
                }
                else
                {
                    _units += 1;
                }
            }
        }
    }
}