using Base.Threads
using Random

Random.seed!(1234)

function partition!(a, lo, hi)
    pivot = a[hi]
    i = lo - 1
    for j in lo:hi-1
        if a[j] ≤ pivot
            i += 1
            a[i], a[j] = a[j], a[i]
        end
    end
    a[i+1], a[hi] = a[hi], a[i+1]
    return i + 1
end

function pqsort!(a::Vector{Int}, lo::Int=1, hi::Int=length(a))
    if lo >= hi
        return
    end

    p = partition!(a, lo, hi)

    if hi - lo > 100000000
        t = @spawn pqsort!(a, lo, p - 1)
        pqsort!(a, p + 1, hi)
        fetch(t)
    else
        pqsort!(a, lo, p - 1)
        pqsort!(a, p + 1, hi)
    end
end

println("start")

a = rand(1:1000, 100)
pqsort!(a)

b = rand(1:1000000, 100000000)
t = @elapsed pqsort!(b)

#println(b)
println("Dauer: ", t)