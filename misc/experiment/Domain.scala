package nyaya.test

import scala.collection.immutable.NumericRange
import scalaz.{Functor, \/-, -\/, \/}

trait Domain[A] {
  val size: Int
  def apply(i: Int): A

  def subst[B >: A]: Domain[B] =
    map(a => a)

  def map[B](f: A => B): Domain[B] =
    new Domain.Mapped(this, f)

  def option: Domain[Option[A]] =
    new Domain.OptionT(this)

  def either[B](b: Domain[B]): Domain[Either[A, B]] =
    (this +++ b).map(_.toEither)

  def +++[B](b: Domain[B]): Domain[A \/ B] =
    new Domain.Disjunction(this, b)

  def ***[B](b: Domain[B]): Domain[(A, B)] =
    new Domain.Pair(this, b)

  def toStream: Stream[A] =
    (0 until size).toStream.map(apply)
}

object Domain {

  implicit val domainInstance: Functor[Domain] = new Functor[Domain] {
    override def map[A, B](fa: Domain[A])(f: A => B): Domain[B] = fa map f
  }

  final class Mapped[A, B](u: Domain[A], f: A => B) extends Domain[B] {
    override val size = u.size
    override def apply(i: Int) = f(u(i))
  }

  final class OptionT[A](u: Domain[A]) extends Domain[Option[A]] {
    override val size = u.size + 1
    override def apply(i: Int) = if (i == 0) None else Some(u(i - 1))
  }

  final class Disjunction[A, B](a: Domain[A], b: Domain[B]) extends Domain[A \/ B] {
    private[this] val as = a.size
    override val size = as + b.size
    override def apply(i: Int) = if (i < as) -\/(a(i)) else \/-(b(i - as))
  }

  final class Pair[A, B](a: Domain[A], b: Domain[B]) extends Domain[(A, B)] {
    private[this] val as = a.size
    override val size = as * b.size
    override def apply(i: Int) = (a(i % as), b(i / as))
  }

  final class OverSeq[A](as: IndexedSeq[A]) extends Domain[A] {
    override val size = as.length
    override def apply(i: Int) = as(i)
  }

  def ofValues[A](as: A*): Domain[A] =
    new OverSeq[A](as.toIndexedSeq)

  def ofRange(r: Range): Domain[Int] =
    new OverSeq[Int](r)

  def ofRangeN[A](r: NumericRange[A]): Domain[A] =
    new OverSeq[A](r)

  val boolean: Domain[Boolean] =
    ofValues(true, false)

  lazy val byte: Domain[Byte] =
    ofRange(Byte.MinValue to Byte.MaxValue).map(_.toByte)
}
